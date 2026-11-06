package com.peach.gateway.core;

import com.peach.gateway.core.config.GatewayRedisProperties;
import com.peach.gateway.core.dao.PeachSaTokenDao;
import com.peach.gateway.core.filter.GatewayIpWhitelistGlobalFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 网关只保留一条 Lettuce 连接的启动测试。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/06
 */
class GatewayRateLimitRedisConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    RedisAutoConfiguration.class,
                    RedisReactiveAutoConfiguration.class))
            .withUserConfiguration(
                    GatewayRateLimitRedisConfiguration.class,
                    GatewayIpWhitelistGlobalFilter.class)
            .withPropertyValues(
                    "peach.redis.mode=standalone",
                    "peach.redis.host=127.0.0.1:6379",
                    "peach.redis.password=",
                    "peach.redis.database=2",
                    "spring.data.redis.host=127.0.0.1",
                    "spring.data.redis.port=6379");

    @Test
    void startsWithOneLettuceConnectionForReactiveAndSaToken() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ReactiveStringRedisTemplate.class);
            assertThat(context.getBeanNamesForType(ReactiveStringRedisTemplate.class))
                    .containsExactly(GatewayRateLimitRedisConfiguration.REACTIVE_STRING_REDIS_TEMPLATE_BEAN_NAME);

            ReactiveStringRedisTemplate reactiveTemplate = context.getBean(ReactiveStringRedisTemplate.class);
            LettuceConnectionFactory lettuceFactory = context.getBean(
                    GatewayRateLimitRedisConfiguration.REACTIVE_CONNECTION_FACTORY_BEAN_NAME,
                    LettuceConnectionFactory.class);
            assertThat(context.getBeanNamesForType(RedisConnectionFactory.class))
                    .containsExactly(GatewayRateLimitRedisConfiguration.REACTIVE_CONNECTION_FACTORY_BEAN_NAME);
            assertThat(reactiveTemplate.getConnectionFactory()).isSameAs(lettuceFactory);
            assertThat(lettuceFactory.getStandaloneConfiguration().getHostName()).isEqualTo("127.0.0.1");
            assertThat(lettuceFactory.getStandaloneConfiguration().getPort()).isEqualTo(6379);
            assertThat(lettuceFactory.getStandaloneConfiguration().getDatabase()).isEqualTo(2);
            assertThat(context.getBean(StringRedisTemplate.class).getConnectionFactory()).isSameAs(lettuceFactory);

            GatewayIpWhitelistGlobalFilter whitelistFilter = context.getBean(GatewayIpWhitelistGlobalFilter.class);
            assertThat(redisTemplate(whitelistFilter)).isSameAs(reactiveTemplate);
            assertThat(new PeachSaTokenDao(lettuceFactory)).isNotNull();
        });
    }

    @Test
    void readsSentinelClusterAndSslFromSharedSettings() {
        GatewayRedisProperties properties = new GatewayRedisProperties();
        properties.setHost("10.0.0.1:26379, 10.0.0.2:26379");
        properties.setPassword("secret");
        properties.setDatabase(1);
        properties.setSentinelMaster("peach");
        properties.getRedisson().setSslEnable(true);

        RedisSentinelConfiguration sentinel = properties.sentinelConfiguration();
        assertThat(sentinel.getMaster().getName()).isEqualTo("peach");
        assertThat(sentinel.getSentinels()).hasSize(2);
        assertThat(sentinel.getDatabase()).isEqualTo(1);
        assertThat(sentinel.getPassword().isPresent()).isTrue();

        RedisClusterConfiguration cluster = properties.redisClusterConfiguration();
        assertThat(cluster.getClusterNodes()).hasSize(2);
        assertThat(cluster.getMaxRedirects()).isEqualTo(5);
        assertThat(properties.isSslEnabled()).isTrue();
    }

    private static Object redisTemplate(GatewayIpWhitelistGlobalFilter filter) {
        try {
            Field field = GatewayIpWhitelistGlobalFilter.class.getDeclaredField("redisTemplate");
            field.setAccessible(true);
            return field.get(filter);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot read whitelist filter Redis template", exception);
        }
    }
}
