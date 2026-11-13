package com.peach.rocket.jdbc;

import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.idempotent.MqIdempotentClaim;
import com.peach.rocket.idempotent.MqIdempotentContext;
import com.peach.rocket.outbox.MqOutboxClaim;
import com.peach.rocket.outbox.MqOutboxEvent;
import com.peach.rocket.outbox.MqOutboxStatus;
import java.sql.Connection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 真实 MySQL 方言与令牌检查；仅允许显式提供空的专用测试库。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13
 */
@EnabledIfSystemProperty(named = "peach.mysql.integration", matches = "true")
class JdbcMySqlIntegrationTest {

    @Test
    void mysqlLeaseFencingExpiryAndCorruptionIsolation() throws Exception {
        String url = System.getenv("PEACH_MYSQL_IT_URL");
        if (url == null || !url.matches("jdbc:mysql://[^/]+/peach_rocket_it\\w*(?:[?].*)?")) {
            throw new IllegalStateException(
                    "mysql-integration requires PEACH_MYSQL_IT_URL naming a dedicated peach_rocket_it database");
        }
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                url, System.getenv("PEACH_MYSQL_IT_USER"), System.getenv("PEACH_MYSQL_IT_PASSWORD"));
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()", Integer.class))
                .as("Integration schema must be empty")
                .isZero();
        Path root = Path.of("").toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("sql/MQ_CONSUME_RECORD.sql"))) {
            root = root.getParent();
        }
        if (root == null) {
            throw new IllegalStateException("Repository SQL directory not found");
        }
        try {
            try (Connection connection = dataSource.getConnection()) {
                ScriptUtils.executeSqlScript(connection, new FileSystemResource(root.resolve("sql/MQ_CONSUME_RECORD.sql")));
                ScriptUtils.executeSqlScript(connection, new FileSystemResource(root.resolve("sql/MQ_OUTBOX_EVENT.sql")));
            }
            assertThat(jdbc.queryForList(
                    "SELECT TABLE_NAME, ENGINE FROM information_schema.TABLES "
                            + "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('MQ_CONSUME_RECORD','MQ_OUTBOX_EVENT')"))
                    .as("New MQ tables must use a transactional engine")
                    .allSatisfy(row -> assertThat(row.get("ENGINE")).isEqualTo("InnoDB"));
            JdbcMqIdempotentStore idempotent = new JdbcMqIdempotentStore(jdbc, new PeachRocketProperties());
            MqIdempotentContext context = new MqIdempotentContext(
                    "key", "group", "topic", null, null, "id", Duration.ofHours(1));
            MqIdempotentClaim owner = idempotent.claim(context);
            assertThat(owner.status()).isEqualTo(MqIdempotentClaim.Status.ACQUIRED);
            assertThat(idempotent.renew(context, owner)).isTrue();
            idempotent.markSuccess(context, owner);
            assertThat(idempotent.claim(context).status()).isEqualTo(MqIdempotentClaim.Status.SUCCEEDED);
            jdbc.update("UPDATE MQ_CONSUME_RECORD SET EXPIRES_AT=DATE_SUB(NOW(3), INTERVAL 1 SECOND)");
            assertThat(idempotent.claim(context).status()).isEqualTo(MqIdempotentClaim.Status.ACQUIRED);

            JdbcMqOutboxStore outbox = new JdbcMqOutboxStore(jdbc, new PeachRocketProperties());
            LocalDateTime now = LocalDateTime.now();
            outbox.save(new MqOutboxEvent("good", new byte[]{1}, "topic", null, null, null, MqOutboxStatus.INIT, 0, now, now));
            outbox.save(new MqOutboxEvent("bad", new byte[]{1}, "topic", null, null, null, MqOutboxStatus.INIT, 0, now, now));
            jdbc.update("UPDATE MQ_OUTBOX_EVENT SET PAYLOAD='!invalid!' WHERE MESSAGE_ID='bad'");
            List<MqOutboxClaim> claims = outbox.claimPending(2);
            assertThat(claims).hasSize(1);
            assertThat(jdbc.queryForObject(
                    "SELECT STATUS FROM MQ_OUTBOX_EVENT WHERE MESSAGE_ID='bad'", String.class)).isEqualTo("FAILED");
            MqOutboxClaim claim = claims.getFirst();
            assertThat(outbox.renew(claim)).isTrue();
            outbox.markSent(claim);
            assertThat(outbox.renew(claim)).isFalse();
        } finally {
            jdbc.execute("DROP TABLE IF EXISTS MQ_OUTBOX_EVENT");
            jdbc.execute("DROP TABLE IF EXISTS MQ_CONSUME_RECORD");
        }
    }
}
