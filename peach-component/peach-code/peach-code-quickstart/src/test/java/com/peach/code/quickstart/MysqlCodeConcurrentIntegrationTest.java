package com.peach.code.quickstart;

import com.peach.code.CodeGeneratorException;
import com.peach.code.autoconfigure.CodeProperties;
import com.peach.code.autoconfigure.PeachCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 使用真实 InnoDB 行锁验证发号唯一性，并模拟 UPDATE 持久化故障。
 *
 * <p>依赖可用 Docker，缺少时 Testcontainers 显式跳过；目标环境迁移仍需运维核对历史序号。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/11
 */
@Testcontainers(disabledWithoutDocker = true)
class MysqlCodeConcurrentIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("peach_code_test")
            .withUsername("peach")
            .withPassword("peach-test");

    private JdbcTemplate jdbc;
    private PeachCodeGenerator first;
    private PeachCodeGenerator second;

    @BeforeEach
    void prepareRule() {
        DataSource dataSource = new DriverManagerDataSource(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE IF NOT EXISTS PEACH_CODE_RULE ("
                + "TENANT_ID VARCHAR(32) NOT NULL,"
                + "CODE_PREFIX VARCHAR(32) NOT NULL,"
                + "MAX_CODE_WIDTH INT NOT NULL,"
                + "CURRENT_VALUE BIGINT NOT NULL,"
                + "STATUS VARCHAR(16) NOT NULL,"
                + "MODIFY_TIME VARCHAR(20),"
                + "PRIMARY KEY (TENANT_ID, CODE_PREFIX)"
                + ") ENGINE=InnoDB");
        jdbc.execute("DROP TRIGGER IF EXISTS PEACH_CODE_FAIL_UPDATE");
        jdbc.update("DELETE FROM PEACH_CODE_RULE");
        jdbc.update("INSERT INTO PEACH_CODE_RULE(TENANT_ID,CODE_PREFIX,MAX_CODE_WIDTH,CURRENT_VALUE,STATUS)"
                        + " VALUES ('T001','MENU',8,0,'ENABLE')");
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
        first = new PeachCodeGenerator(jdbc, new CodeProperties(), null, transactionManager);
        second = new PeachCodeGenerator(jdbc, new CodeProperties(), null, transactionManager);
    }

    @Test
    void twoInstancesMustAllocateEachNumberExactlyOnce() throws Exception {
        final int count = 32;
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> pending = new ArrayList<>();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < count; i++) {
                PeachCodeGenerator instance = i % 2 == 0 ? first : second;
                pending.add(executor.submit(() -> {
                    if (!start.await(20, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("MySQL concurrency gate timeout");
                    }
                    return instance.next("T001", "menu");
                }));
            }
            start.countDown();
            List<String> allocated = new ArrayList<>();
            for (Future<String> value : pending) {
                allocated.add(value.get(30, TimeUnit.SECONDS));
            }
            assertThat(new HashSet<>(allocated)).hasSize(count);
            for (int i = 1; i <= count; i++) {
                assertThat(allocated).contains(String.format("MENU_%08d", i));
            }
            Long stored = jdbc.queryForObject("SELECT CURRENT_VALUE FROM PEACH_CODE_RULE WHERE TENANT_ID='T001' AND CODE_PREFIX='MENU'", Long.class);
            assertThat(stored).isEqualTo((long) count);
        }
    }

    @Test
    void injectedDatabaseUpdateFailureMustNotReturnASequenceOrFallbackToRedis() {
        assertThat(first.next("T001", "MENU")).isEqualTo("MENU_00000001");
        jdbc.execute("CREATE TRIGGER PEACH_CODE_FAIL_UPDATE BEFORE UPDATE ON PEACH_CODE_RULE "
                + "FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='injected allocation outage'");
        try {
            assertThatThrownBy(() -> second.next("T001", "MENU"))
                    .isInstanceOf(CodeGeneratorException.class);
            assertThat(jdbc.queryForObject("SELECT CURRENT_VALUE FROM PEACH_CODE_RULE", Long.class)).isEqualTo(1L);
        } finally {
            jdbc.execute("DROP TRIGGER IF EXISTS PEACH_CODE_FAIL_UPDATE");
        }
        assertThat(second.next("T001", "MENU")).isEqualTo("MENU_00000002");
    }
}
