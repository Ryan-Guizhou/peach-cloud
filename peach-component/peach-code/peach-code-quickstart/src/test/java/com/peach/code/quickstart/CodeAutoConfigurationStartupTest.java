package com.peach.code.quickstart;

import com.peach.code.CodeGenerator;
import com.peach.code.autoconfigure.PeachCodeAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 验证纯 JDBC 编码 Starter 在无 Redis、无数据源时不会产生缺失 Bean 的启动异常。
 *
 * <p>可用数据源与事务管理器同时存在时，才注册默认编码服务。</p>
 */
class CodeAutoConfigurationStartupTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PeachCodeAutoConfiguration.class));

    @Test
    void shouldBackOffWithoutJdbcInsteadOfFailingApplicationContext() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(CodeGenerator.class);
        });
    }

    @Test
    void shouldProvideCodeGeneratorWhenJdbcAndTransactionsExist() {
        runner.withBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class))
                .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(CodeGenerator.class);
                });
    }
}
