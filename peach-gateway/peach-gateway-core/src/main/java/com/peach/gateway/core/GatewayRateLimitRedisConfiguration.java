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
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/05
 */
@Configuration(proxyBeanMethods = false)
public class GatewayRateLimitRedisConfiguration {

    @Bean(name = "gatewayRateLimitReactiveRedisConnectionFactory", destroyMethod = "destroy")
    @ConditionalOnMissingBean(name = "gatewayRateLimitReactiveRedisConnectionFactory")
    public LettuceConnectionFactory gatewayRateLimitReactiveRedisConnectionFactory(RedisConfig redisConfig) {
        LettuceClientConfiguration clientConfiguration = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(2))
                .shutdownTimeout(Duration.ZERO)
                .build();

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

    @Bean(name = "gatewayRateLimitReactiveStringRedisTemplate")
    @ConditionalOnMissingBean(name = "gatewayRateLimitReactiveStringRedisTemplate")
    public ReactiveStringRedisTemplate gatewayRateLimitReactiveStringRedisTemplate(
            @Qualifier("gatewayRateLimitReactiveRedisConnectionFactory")
            ReactiveRedisConnectionFactory connectionFactory) {
        return new ReactiveStringRedisTemplate(connectionFactory);
    }
}
