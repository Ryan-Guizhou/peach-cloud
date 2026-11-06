package com.peach.gateway.core;

import com.peach.gateway.core.config.GatewayRedisProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
 * <p>网关只创建这一条 Lettuce 连接。地址、密码和模式来自共享的 {@code peach.redis.*}，
 * 由 {@link GatewayRedisProperties} 解析。限流和过滤器使用响应式 API；
 * Sa-Token DAO 复用同一个工厂的阻塞 API，并在 boundedElastic 上执行。</p>
 *
 * <p>响应式字符串模板使用 Spring Boot 标准 Bean 名称 {@code reactiveStringRedisTemplate}。
 * 这样自动配置不会再注册第二个同类型模板，按类型注入和限定注入解析到同一个 Bean。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/05
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GatewayRedisProperties.class)
public class GatewayRateLimitRedisConfiguration {

    /** Spring Boot 标准响应式字符串模板 Bean 名称，过滤器与限流执行器统一限定到该 Bean。 */
    public static final String REACTIVE_STRING_REDIS_TEMPLATE_BEAN_NAME = "reactiveStringRedisTemplate";

    /** 网关唯一的 Lettuce 连接工厂 Bean 名称。 */
    public static final String REACTIVE_CONNECTION_FACTORY_BEAN_NAME = "gatewayRateLimitReactiveRedisConnectionFactory";

    @Bean(name = REACTIVE_CONNECTION_FACTORY_BEAN_NAME, destroyMethod = "destroy")
    @ConditionalOnMissingBean(name = REACTIVE_CONNECTION_FACTORY_BEAN_NAME)
    public LettuceConnectionFactory gatewayRateLimitReactiveRedisConnectionFactory(GatewayRedisProperties redisProperties) {
        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientBuilder =
                LettuceClientConfiguration.builder()
                        .commandTimeout(Duration.ofSeconds(2))
                        .shutdownTimeout(Duration.ZERO);
        LettuceClientConfiguration clientConfiguration = redisProperties.isSslEnabled()
                ? clientBuilder.useSsl().build()
                : clientBuilder.build();

        String mode = redisProperties.getMode();
        if (mode == null || mode.isBlank()) {
            throw new IllegalArgumentException("peach.redis.mode is required");
        }
        return switch (mode) {
            case GatewayRedisProperties.STANDALONE -> new LettuceConnectionFactory(
                    redisProperties.redisStandaloneConfiguration(), clientConfiguration);
            case GatewayRedisProperties.SENTINEL -> new LettuceConnectionFactory(
                    redisProperties.sentinelConfiguration(), clientConfiguration);
            case GatewayRedisProperties.CLUSTER -> new LettuceConnectionFactory(
                    redisProperties.redisClusterConfiguration(), clientConfiguration);
            default -> throw new IllegalArgumentException("Unsupported peach.redis.mode for gateway Redis: " + mode);
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
