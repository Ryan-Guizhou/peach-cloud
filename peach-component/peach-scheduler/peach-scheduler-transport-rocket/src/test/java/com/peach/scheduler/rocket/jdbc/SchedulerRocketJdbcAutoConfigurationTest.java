package com.peach.scheduler.rocket.jdbc;

import javax.sql.DataSource;

import com.peach.rocket.idempotent.MqIdempotentStore;
import com.peach.rocket.outbox.MqOutboxStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SchedulerRocketJdbcAutoConfiguration} 自动装配测试。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/8/24
 */
class SchedulerRocketJdbcAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    SchedulerRocketJdbcAutoConfiguration.class,
                    JdbcTemplateAutoConfiguration.class))
            .withUserConfiguration(DataSourceConfiguration.class);

    /**
     * 验证 DataSource 可用时应在通用 RocketMQ 配置之前装配 JDBC 持久化 Store。
     */
    @Test
    void shouldConfigureJdbcStoresAfterJdbcTemplateAutoConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(MqOutboxStore.class);
            assertThat(context).hasSingleBean(MqIdempotentStore.class);
            assertThat(context.getBean(MqOutboxStore.class)).isInstanceOf(SchedulerJdbcMqOutboxStore.class);
            assertThat(context.getBean(MqIdempotentStore.class)).isInstanceOf(SchedulerJdbcMqIdempotentStore.class);
        });
    }

    /**
     * 测试数据源配置，仅用于验证自动配置顺序，不建立真实数据库连接。
     */
    @Configuration(proxyBeanMethods = false)
    static class DataSourceConfiguration {

        /**
         * 创建测试数据源。
         *
         * @return 测试数据源
         */
        @Bean
        DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:peach-test:memory");
        }
    }
}
