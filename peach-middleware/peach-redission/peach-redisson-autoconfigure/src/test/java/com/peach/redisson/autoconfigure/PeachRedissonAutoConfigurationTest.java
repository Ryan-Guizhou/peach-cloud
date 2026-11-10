package com.peach.redisson.autoconfigure;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SentinelServersConfig;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 验证 Redisson-only 自动装配在外部客户端或关闭开关存在时不会创建额外连接。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
class PeachRedissonAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PeachRedissonAutoConfiguration.class));

    @Test
    void userSuppliedRedissonClientTakesPrecedence() {
        RedissonClient existing = mock(RedissonClient.class);
        runner.withBean(RedissonClient.class, () -> existing)
                .withPropertyValues("peach.redis.host=127.0.0.1:6379")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBeansOfType(RedissonClient.class)).hasSize(1);
                    assertThat(context.getBean(RedissonClient.class)).isSameAs(existing);
                });
    }

    @Test
    void disablingRedissonMustNotCreateClient() {
        runner.withPropertyValues("peach.redis.redisson.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBeansOfType(RedissonClient.class)).isEmpty();
                });
    }

    @Test
    void sentinelAuthIsSeparateFromDataNodeAuth() {
        PeachRedissonProperties properties = new PeachRedissonProperties();
        properties.setUsername("data-user");
        properties.setPassword("data-secret");
        properties.setSentinelUsername("monitor-user");
        properties.setSentinelPassword("monitor-secret");

        Config config = new Config();
        PeachRedissonAutoConfiguration.configureSentinel(config, properties, properties.getRedisson(),
                new String[]{"redis://127.0.0.1:26379"});
        SentinelServersConfig sentinel = config.useSentinelServers();
        assertThat(sentinel.getUsername()).isEqualTo("data-user");
        assertThat(sentinel.getPassword()).isEqualTo("data-secret");
        assertThat(sentinel.getSentinelUsername()).isEqualTo("monitor-user");
        assertThat(sentinel.getSentinelPassword()).isEqualTo("monitor-secret");
    }

    @Test
    void redissonClusterRejectsNonzeroDatabaseBeforeClientConnect() {
        PeachRedissonProperties properties = new PeachRedissonProperties();
        properties.setDatabase(1);
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                PeachRedissonAutoConfiguration.configureCluster(
                        new Config(), properties, properties.getRedisson(),
                        new String[]{"redis://127.0.0.1:7001"}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("database 0");
    }

}
