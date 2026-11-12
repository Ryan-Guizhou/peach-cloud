package com.peach.rocket.jdbc;

import com.peach.rocket.idempotent.MqIdempotentStore;
import com.peach.rocket.outbox.MqOutboxStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 验证通用 JDBC 存储必须显式启用，避免业务服务误使用数据库。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/12
 */
class PeachRocketJdbcAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PeachRocketJdbcAutoConfiguration.class))
            .withBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class));

    @Test
    void shouldStayDisabledWithoutExplicitOptIn() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(MqOutboxStore.class);
            assertThat(context).doesNotHaveBean(MqIdempotentStore.class);
        });
    }

    @Test
    void shouldCreateDurableStoresWhenEnabled() {
        runner.withPropertyValues("peach.rocket.jdbc.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(MqOutboxStore.class);
                    assertThat(context).hasSingleBean(MqIdempotentStore.class);
                    assertThat(context.getBean(MqOutboxStore.class))
                            .isInstanceOf(JdbcMqOutboxStore.class);
                    assertThat(context.getBean(MqIdempotentStore.class))
                            .isInstanceOf(JdbcMqIdempotentStore.class);
                });
    }
}
