package com.peach.code.autoconfigure;

import com.peach.code.CodeGeneratorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 使用真实 JDBC 事务验证配置变更、账本、回滚与并发；生产 InnoDB 另有容器测试。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/23 11:00
 */
class MysqlCodeAllocationTest {

    private JdbcTemplate jdbc;
    private DataSourceTransactionManager manager;
    private PeachCodeGenerator generator;

    @BeforeEach
    void setup() {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        manager = new DataSourceTransactionManager(dataSource);
        generator = new PeachCodeGenerator(jdbc, new CodeProperties(), manager);
        jdbc.execute("CREATE TABLE PEACH_CODE_RULE (TENANT_ID VARCHAR(32),CODE_TYPE VARCHAR(32),"
                + "CODE_PREFIX VARCHAR(32),MAX_CODE_WIDTH INT,CURRENT_VALUE BIGINT,STATUS VARCHAR(16),"
                + "MODIFY_TIME VARCHAR(23),PRIMARY KEY(TENANT_ID,CODE_TYPE))");
        jdbc.execute("CREATE TABLE PEACH_CODE_ISSUED(TENANT_ID VARCHAR(32),CODE_TYPE VARCHAR(32),"
                + "CODE_VALUE VARCHAR(64),SEQUENCE_VALUE BIGINT,CREATED_TIME VARCHAR(23),PRIMARY KEY(TENANT_ID,CODE_VALUE))");
        jdbc.update("INSERT INTO PEACH_CODE_RULE VALUES('T001','MENU','M',5,0,'ENABLE',NULL)");
    }

    @Test
    void directRuleEditsApplyWithoutRestartAndDoNotResetSequence() {
        assertThat(generator.next("T001", "menu")).isEqualTo("M00001");
        jdbc.update("UPDATE PEACH_CODE_RULE SET CODE_PREFIX='NAV',MAX_CODE_WIDTH=3,CURRENT_VALUE=0");
        assertThat(generator.next("T001", "MENU")).isEqualTo("NAV002");
        jdbc.update("UPDATE PEACH_CODE_RULE SET CODE_PREFIX='M',MAX_CODE_WIDTH=5,CURRENT_VALUE=0");
        assertThat(generator.next("T001", "MENU")).isEqualTo("M00003");
        assertThat(jdbc.queryForList("SELECT CODE_VALUE FROM PEACH_CODE_ISSUED", String.class))
                .containsExactlyInAnyOrder("M00001", "NAV002", "M00003");
    }

    @Test
    void collisionWithHistoricalFormatFailsAndRollsBackWatermark() {
        assertThat(generator.next("T001", "MENU")).isEqualTo("M00001");
        jdbc.update("UPDATE PEACH_CODE_RULE SET CODE_PREFIX='M0000',MAX_CODE_WIDTH=1");
        // Seed an older code whose rendered form collides after prefix/width editing.
        jdbc.update("INSERT INTO PEACH_CODE_ISSUED VALUES('T001','ROLE','M00002',1,NULL)");
        assertThatThrownBy(() -> generator.next("T001", "MENU")).isInstanceOf(CodeGeneratorException.class);
        assertThat(jdbc.queryForObject("SELECT CURRENT_VALUE FROM PEACH_CODE_RULE", Long.class)).isEqualTo(1L);
    }

    @Test
    void overflowRejectsWithoutReservingAnything() {
        jdbc.update("UPDATE PEACH_CODE_RULE SET MAX_CODE_WIDTH=1,CURRENT_VALUE=9");
        assertThatThrownBy(() -> generator.next("T001", "MENU"))
                .isInstanceOf(CodeGeneratorException.class).hasMessageContaining("MAX_CODE_WIDTH");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM PEACH_CODE_ISSUED", Long.class)).isZero();
    }

    @Test
    void businessRollbackDoesNotRecycleCommittedAllocation() {
        new TransactionTemplate(manager).executeWithoutResult(transaction -> {
            assertThat(generator.next("T001", "MENU")).isEqualTo("M00001");
            transaction.setRollbackOnly();
        });
        assertThat(generator.next("T001", "MENU")).isEqualTo("M00002");
    }

    @Test
    void redisAheadValueMustBeCommittedAndLossCannotRecycleIt() {
        var coordinated = new PeachCodeGenerator(jdbc, new CodeProperties(), manager,
                (tenant, type, durable, max) -> 42L);
        assertThat(coordinated.next("T001", "MENU")).isEqualTo("M00042");
        assertThat(generator.next("T001", "MENU")).isEqualTo("M00043");
        jdbc.update("UPDATE PEACH_CODE_RULE SET CURRENT_VALUE=0");
        assertThat(generator.next("T001", "MENU")).isEqualTo("M00044");
    }

    @Test
    void redisFailureKeepsUsingTheSameDurableAllocatorAndRecoversFromItsWatermark() {
        var redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenThrow(new DataAccessResourceFailureException("Injected Redis outage"));
        var coordinated = new PeachCodeGenerator(jdbc, new CodeProperties(), manager,
                new RedisCodeSequenceCoordinator(redis, new CodeProperties()));
        assertThat(coordinated.next("T001", "MENU")).isEqualTo("M00001");
        assertThat(generator.next("T001", "MENU")).isEqualTo("M00002");
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn("3");
        assertThat(coordinated.next("T001", "MENU")).isEqualTo("M00003");
    }

    @Test
    void databaseFailureAfterRedisReservationRollsBackLedgerAndRecoverySkipsReservation() {
        jdbc.execute("ALTER TABLE PEACH_CODE_RULE ADD CONSTRAINT BLOCK_ALLOCATION CHECK(CURRENT_VALUE=0)");
        var coordinated = new PeachCodeGenerator(jdbc, new CodeProperties(), manager,
                (tenant, type, durable, max) -> 42L);
        assertThatThrownBy(() -> coordinated.next("T001", "MENU")).isInstanceOf(CodeGeneratorException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM PEACH_CODE_ISSUED", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT CURRENT_VALUE FROM PEACH_CODE_RULE", Long.class)).isZero();
        jdbc.execute("ALTER TABLE PEACH_CODE_RULE DROP CONSTRAINT BLOCK_ALLOCATION");
        var recovered = new PeachCodeGenerator(jdbc, new CodeProperties(), manager,
                (tenant, type, durable, max) -> 43L);
        assertThat(recovered.next("T001", "MENU")).isEqualTo("M00043");
        assertThat(generator.next("T001", "MENU")).isEqualTo("M00044");
    }

    @Test
    void disabledMissingAndInvalidRulesFailClosed() {
        jdbc.update("UPDATE PEACH_CODE_RULE SET STATUS='DISABLE'");
        assertThatThrownBy(() -> generator.next("T001", "MENU")).isInstanceOf(CodeGeneratorException.class);
        assertThatThrownBy(() -> generator.next("T001", "ROLE")).isInstanceOf(CodeGeneratorException.class);
        assertThatThrownBy(() -> generator.next("T001", "MENU_X")).hasMessageContaining("codeType");
        jdbc.update("UPDATE PEACH_CODE_RULE SET STATUS='ENABLE',CODE_PREFIX='bad_prefix'");
        assertThatThrownBy(() -> generator.next("T001", "MENU")).hasMessageContaining("CODE_PREFIX");
    }

    @Test
    void eighteenDigitNumbersRemainExact() {
        jdbc.update("UPDATE PEACH_CODE_RULE SET MAX_CODE_WIDTH=18,CURRENT_VALUE=999999999999999998");
        assertThat(generator.next("T001", "MENU")).isEqualTo("M999999999999999999");
        assertThatThrownBy(() -> generator.next("T001", "MENU")).hasMessageContaining("MAX_CODE_WIDTH");
    }

    @Test
    void twoInstancesSerializeAllocationsAndTenantsRemainIsolated() throws Exception {
        jdbc.update("INSERT INTO PEACH_CODE_RULE VALUES('T002','MENU','M',5,0,'ENABLE',NULL)");
        var other = new PeachCodeGenerator(jdbc, new CodeProperties(), manager);
        var start = new CountDownLatch(1);
        List<Future<String>> pending = new ArrayList<>();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 32; i++) {
                var instance = i % 2 == 0 ? generator : other;
                pending.add(executor.submit(() -> {
                    if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start timeout");
                    return instance.next("T001", "MENU");
                }));
            }
            start.countDown();
            List<String> codes = new ArrayList<>();
            for (var future : pending) codes.add(future.get(15, TimeUnit.SECONDS));
            assertThat(new HashSet<>(codes)).hasSize(32);
        }
        assertThat(other.next("T002", "MENU")).isEqualTo("M00001");
        assertThat(generator.next("T001", "MENU")).isEqualTo("M00033");
    }
}
