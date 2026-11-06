package com.peach.gateway.core;

import com.peach.redis.common.RedisConfig;
import com.peach.redis.constant.RedisConstant;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

import java.time.Duration;

/**
 * 网关响应式 Redis 连接配置。
 *
 * <p>现有业务 Redis 继续使用 Jedis；网关限流和响应式过滤器使用独立 Lettuce 连接，
 * 共享同一套 peach.redis 连接参数，避免在 Netty EventLoop 中执行阻塞 Redis 操作。</p>
 *
 * <p>响应式字符串模板使用 Spring Boot 标准 Bean 名称 {@code reactiveStringRedisTemplate}。
 * 这样自动配置不会再注册第二个同类型模板，按类型注入和限定注入解析到同一个 Bean。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/05
 */
@Configuration(proxyBeanMethods = false)
public class GatewayRateLimitRedisConfiguration {

    /** Spring Boot 标准响应式字符串模板 Bean 名称，过滤器与限流执行器统一限定到该 Bean。 */
    public static final String REACTIVE_STRING_REDIS_TEMPLATE_BEAN_NAME = "reactiveStringRedisTemplate";

    /** 网关限流专用 Lettuce 连接工厂 Bean 名称。 */
    public static final String REACTIVE_CONNECTION_FACTORY_BEAN_NAME = "gatewayRateLimitReactiveRedisConnectionFactory";

    @Bean(name = REACTIVE_CONNECTION_FACTORY_BEAN_NAME, destroyMethod = "destroy")
    @ConditionalOnMissingBean(name = REACTIVE_CONNECTION_FACTORY_BEAN_NAME)
    public LettuceConnectionFactory gatewayRateLimitReactiveRedisConnectionFactory(RedisConfig redisConfig) {
        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientBuilder =
                LettuceClientConfiguration.builder()
                        .commandTimeout(Duration.ofSeconds(2))
                        .shutdownTimeout(Duration.ZERO);
        LettuceClientConfiguration clientConfiguration = redisConfig.isSslEnable()
                ? clientBuilder.useSsl().build()
                : clientBuilder.build();

        return switch (redisConfig.getMode()) {
            case RedisConstant.STANDALONE -> new LettuceConnectionFactory(
                    redisConfig.redisStandaloneConfiguration(), clientConfiguration);
            case RedisConstant.SENTINEL -> new LettuceConnectionFactory(
                    redisConfig.sentinelConfiguration(), clientConfiguration);
            case RedisConstant.CLUSTER -> new LettuceConnectionFactory(
                    redisConfig.redisClusterConfiguration(), clientConfiguration);
            default -> throw new IllegalArgumentException("Unsupported peach.redis.mode for reactive gateway Redis");
        };
    }

    @Bean(name = REACTIVE_STRING_REDIS_TEMPLATE_BEAN_NAME)
    @ConditionalOnMissingBean(name = REACTIVE_STRING_REDIS_TEMPLATE_BEAN_NAME)
    public ReactiveStringRedisTemplate reactiveStringRedisTemplate(
            @Qualifier(REACTIVE_CONNECTION_FACTORY_BEAN_NAME)
            ReactiveRedisConnectionFactory connectionFactory) {
        return new ReactiveStringRedisTemplate(connectionFactory);
    }
}
