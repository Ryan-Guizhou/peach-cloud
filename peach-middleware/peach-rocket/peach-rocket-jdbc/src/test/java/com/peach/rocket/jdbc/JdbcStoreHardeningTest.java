package com.peach.rocket.jdbc;

import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.exception.MqException;
import com.peach.rocket.idempotent.MqIdempotentContext;
import com.peach.rocket.outbox.MqOutboxClaim;
import com.peach.rocket.outbox.MqOutboxEvent;
import com.peach.rocket.outbox.MqOutboxStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * H2 仅验证兼容 SQL 子集与本地事务，不代表 MySQL 并发保证。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13
 */
class JdbcStoreHardeningTest {

    @Test
    void synchronizationOnlyConnectionWithAutoCommitDisabledDoesNotOwnBusinessTransaction() {
        JdbcTemplate business = database();
        DriverManagerDataSource outboxSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "") {
            @Override
            public java.sql.Connection getConnection() throws SQLException {
                java.sql.Connection connection = super.getConnection();
                connection.setAutoCommit(false);
                return connection;
            }
        };
        JdbcTemplate outbox = new JdbcTemplate(outboxSource);
        PeachRocketProperties properties = new PeachRocketProperties();
        properties.getOutbox().setRequireTransaction(true);
        JdbcMqOutboxStore store = new JdbcMqOutboxStore(outbox, properties);
        LocalDateTime now = LocalDateTime.now();
        MqOutboxEvent event = new MqOutboxEvent(
                "id", new byte[]{1}, "topic", null, null, null, MqOutboxStatus.INIT, 0, now, now);

        new TransactionTemplate(new DataSourceTransactionManager(business.getDataSource())).execute(status -> {
            assertThat(outbox.queryForObject("SELECT 1", Integer.class)).isEqualTo(1);
            assertThatThrownBy(() -> store.save(event))
                    .isInstanceOf(MqException.class)
                    .hasMessageContaining("transaction");
            assertThatThrownBy(() -> store.saveStandalone(event))
                    .isInstanceOf(MqException.class)
                    .hasMessageContaining("transaction");
            status.setRollbackOnly();
            return null;
        });
        new TransactionTemplate(new DataSourceTransactionManager(outboxSource)).execute(status -> {
            assertThat(JdbcLocalTransactionVerifier.participates(outboxSource)).isTrue();
            status.setRollbackOnly();
            return null;
        });
    }

    @Test
    void legacyPendingRequiresMigrationWithoutClaimingRows() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        assertThatThrownBy(() -> new JdbcMqOutboxStore(jdbc).findPending(1))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("claimPending");
        verifyNoInteractions(jdbc);
    }

    @Test
    void corruptRowDoesNotDiscardValidClaims() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ResultSet valid = mock(ResultSet.class);
        when(valid.getString("MESSAGE_ID")).thenReturn("valid");
        when(valid.getString("TOPIC")).thenReturn("topic");
        when(valid.getString("PAYLOAD")).thenReturn("AQ==");
        when(valid.getString("SEND_MODE")).thenReturn("NORMAL");
        when(valid.getString("STATUS")).thenReturn("SENDING");
        when(valid.wasNull()).thenReturn(true);
        ResultSet corrupt = mock(ResultSet.class);
        when(corrupt.getString("MESSAGE_ID")).thenReturn("corrupt");
        when(corrupt.getString("SEND_MODE")).thenReturn("INVALID");
        when(corrupt.wasNull()).thenReturn(true);
        doAnswer(invocation -> {
            RowMapper<MqOutboxEvent> mapper = invocation.getArgument(1);
            return Arrays.asList(mapper.mapRow(corrupt, 0), mapper.mapRow(valid, 1));
        }).when(jdbc).query(anyString(), any(RowMapper.class), any(), any(), any());

        java.util.List<MqOutboxClaim> claims = new JdbcMqOutboxStore(jdbc).claimPending(2);

        assertThat(claims).hasSize(1);
        assertThat(claims.getFirst().event().messageId()).isEqualTo("valid");
        verify(jdbc).update(contains("INVALID_STORED_EVENT"), eq("corrupt"), anyString());
    }

    @Test
    void legacyCannotCompleteOwnedIdempotentRecord() {
        JdbcTemplate jdbc = database();
        jdbc.execute("CREATE TABLE MQ_CONSUME_RECORD(IDEMPOTENT_KEY VARCHAR(256), CONSUMER_GROUP VARCHAR(128), "
                + "STATUS VARCHAR(32), OWNER_TOKEN VARCHAR(64), LAST_ERROR VARCHAR(1000), UPDATED_AT TIMESTAMP)");
        jdbc.update("INSERT INTO MQ_CONSUME_RECORD VALUES('key','group','PROCESSING','owner',NULL,NOW())");
        JdbcMqIdempotentStore store = new JdbcMqIdempotentStore(jdbc);
        MqIdempotentContext context = new MqIdempotentContext(
                "key", "group", "t", null, null, "m", Duration.ofHours(1));

        store.markFailed(context);

        assertThat(jdbc.queryForObject("SELECT STATUS FROM MQ_CONSUME_RECORD", String.class)).isEqualTo("PROCESSING");
    }

    @Test
    void legacyCannotCompleteOwnedOutboxRecord() {
        JdbcTemplate jdbc = database();
        jdbc.execute("CREATE TABLE MQ_OUTBOX_EVENT(MESSAGE_ID VARCHAR(64), STATUS VARCHAR(32), "
                + "CLAIMED_BY VARCHAR(128), CLAIMED_AT TIMESTAMP, SENT_AT TIMESTAMP, UPDATED_AT TIMESTAMP)");
        jdbc.update("INSERT INTO MQ_OUTBOX_EVENT VALUES('id','SENDING','owner',NOW(),NULL,NOW())");

        new JdbcMqOutboxStore(jdbc).markSent("id");

        assertThat(jdbc.queryForObject("SELECT STATUS FROM MQ_OUTBOX_EVENT", String.class)).isEqualTo("SENDING");
    }

    @Test
    void sameDatasourceTransactionRollsBackOutboxAndStandaloneRejectsWrongDatasource() {
        JdbcTemplate jdbc = database();
        JdbcTemplate other = database();
        jdbc.execute("CREATE TABLE MQ_OUTBOX_EVENT(MESSAGE_ID VARCHAR(64), TOPIC VARCHAR(128), TAG VARCHAR(128), "
                + "BUSINESS_KEY VARCHAR(128), PAYLOAD CLOB, SEND_MODE VARCHAR(32), SHARDING_KEY VARCHAR(128), "
                + "DELAY_LEVEL INT, HEADERS CLOB, SEND_TIMEOUT_MS BIGINT, STATUS VARCHAR(32), RETRY_COUNT INT, "
                + "NEXT_RETRY_TIME TIMESTAMP, CREATED_AT TIMESTAMP, UPDATED_AT TIMESTAMP)");
        PeachRocketProperties properties = new PeachRocketProperties();
        properties.getOutbox().setRequireTransaction(true);
        JdbcMqOutboxStore store = new JdbcMqOutboxStore(jdbc, properties);
        LocalDateTime now = LocalDateTime.now();
        MqOutboxEvent event = new MqOutboxEvent(
                "id", new byte[]{1}, "t", null, null, null, MqOutboxStatus.INIT, 0, now, now);

        assertThatThrownBy(() -> store.save(event)).isInstanceOf(RuntimeException.class);
        new TransactionTemplate(new DataSourceTransactionManager(jdbc.getDataSource())).execute(status -> {
            store.save(event);
            status.setRollbackOnly();
            return null;
        });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM MQ_OUTBOX_EVENT", Integer.class)).isZero();
        new TransactionTemplate(new DataSourceTransactionManager(other.getDataSource())).execute(status -> {
            jdbc.queryForObject("SELECT COUNT(*) FROM MQ_OUTBOX_EVENT", Integer.class);
            assertThatThrownBy(() -> store.save(event)).isInstanceOf(RuntimeException.class);
            assertThatThrownBy(() -> store.saveStandalone(event)).isInstanceOf(RuntimeException.class);
            status.setRollbackOnly();
            return null;
        });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM MQ_OUTBOX_EVENT", Integer.class)).isZero();

        store.saveStandalone(event);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM MQ_OUTBOX_EVENT", Integer.class)).isEqualTo(1);
    }

    private JdbcTemplate database() {
        return new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
    }
}
