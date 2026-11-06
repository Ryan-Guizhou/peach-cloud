package com.peach.gateway.core;

import com.peach.gateway.core.dao.PeachSaTokenDao;
import com.peach.gateway.core.filter.GatewayIpWhitelistGlobalFilter;
import com.peach.redis.common.RedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 网关响应式 Redis 与既有 Jedis 连接的共存测试。
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
                    RedisConfig.class,
                    GatewayRateLimitRedisConfiguration.class,
                    GatewayIpWhitelistGlobalFilter.class)
            .withPropertyValues(
                    "peach.redis.mode=standalone",
                    "peach.redis.host=127.0.0.1:6379",
                    "peach.redis.password=",
                    "peach.redis.redisson.enabled=false",
                    "spring.data.redis.host=127.0.0.1",
                    "spring.data.redis.port=6379");

    @Test
    void startsWithOneReactiveStringTemplateAndKeepsJedisPrimary() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ReactiveStringRedisTemplate.class);
            assertThat(context.getBeanNamesForType(ReactiveStringRedisTemplate.class))
                    .containsExactly(GatewayRateLimitRedisConfiguration.REACTIVE_STRING_REDIS_TEMPLATE_BEAN_NAME);

            ReactiveStringRedisTemplate reactiveTemplate = context.getBean(ReactiveStringRedisTemplate.class);
            LettuceConnectionFactory lettuceFactory = context.getBean(
                    GatewayRateLimitRedisConfiguration.REACTIVE_CONNECTION_FACTORY_BEAN_NAME,
                    LettuceConnectionFactory.class);
            assertThat(reactiveTemplate.getConnectionFactory()).isSameAs(lettuceFactory);

            JedisConnectionFactory jedisFactory = context.getBean(JedisConnectionFactory.class);
            assertThat(context.getBean(RedisConnectionFactory.class)).isSameAs(jedisFactory);
            assertThat(context.getBean(StringRedisTemplate.class).getConnectionFactory()).isSameAs(jedisFactory);
            assertThat(jedisFactory).isNotSameAs(lettuceFactory);

            GatewayIpWhitelistGlobalFilter whitelistFilter = context.getBean(GatewayIpWhitelistGlobalFilter.class);
            assertThat(redisTemplate(whitelistFilter)).isSameAs(reactiveTemplate);
            assertThat(new PeachSaTokenDao(jedisFactory)).isNotNull();
        });
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
