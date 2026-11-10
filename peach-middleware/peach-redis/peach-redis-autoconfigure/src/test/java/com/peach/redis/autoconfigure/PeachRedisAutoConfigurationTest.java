package com.peach.redis.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 自定义 RedisConnectionFactory 退让与可选开关验证。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
class PeachRedisAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PeachRedisAutoConfiguration.class));

    @Test
    void respectsExistingRedisConnectionFactory() {
        RedisConnectionFactory userFactory = mock(RedisConnectionFactory.class);
        runner.withBean(RedisConnectionFactory.class, () -> userFactory)
                .withPropertyValues("peach.redis.host=127.0.0.1:6379")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBeansOfType(RedisConnectionFactory.class)).hasSize(1);
                    assertThat(context.getBean(RedisConnectionFactory.class)).isSameAs(userFactory);
                });
    }

    @Test
    void disabledRedisDoesNotCreateFactory() {
        runner.withPropertyValues("peach.redis.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBeansOfType(RedisConnectionFactory.class)).isEmpty();
                });
    }
}
