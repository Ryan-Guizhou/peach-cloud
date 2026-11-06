package com.peach.gateway.core.ratelimit;

import com.peach.gateway.core.ratelimit.config.GatewayRateLimitProperties;
import com.peach.gateway.core.ratelimit.model.RateLimitAlgorithm;
import com.peach.gateway.core.ratelimit.model.RateLimitDecision;
import com.peach.gateway.core.ratelimit.model.RateLimitRule;
import com.peach.gateway.core.ratelimit.model.RateLimitScope;
import com.peach.gateway.core.ratelimit.script.RateLimitScriptRepository;
import com.peach.gateway.core.ratelimit.service.RateLimitRuleRegistry;
import com.peach.gateway.core.ratelimit.service.RedisRateLimitExecutor;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 可选的真实 Redis Cluster、Sentinel 和 TLS 拓扑冒烟测试。
 *
 * <p>默认未配置 PEACH_REDIS_MODE 时跳过；在目标拓扑环境中设置环境变量后执行。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/06
 */
class RedisTopologySmokeTest {

    @Test
    void shouldExecuteAllRateLimitScriptsOnConfiguredRedisTopology() {
        String mode = System.getenv("PEACH_REDIS_MODE");
        Assumptions.assumeTrue(mode != null && !mode.isBlank(),
                "Set PEACH_REDIS_MODE to standalone, cluster, or sentinel to run this topology test");

        LettuceConnectionFactory factory = createConnectionFactory(mode);
        try {
            factory.afterPropertiesSet();
            ReactiveStringRedisTemplate template = new ReactiveStringRedisTemplate(factory);
            GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
            RateLimitRuleRegistry registry = new RateLimitRuleRegistry(properties);
            RateLimitScriptRepository scripts = new RateLimitScriptRepository(
                    registry, new DefaultResourceLoader());
            scripts.reload().block(Duration.ofSeconds(15));
            RedisRateLimitExecutor executor = new RedisRateLimitExecutor(template, scripts);
            String namespace = "integration:topology:" + UUID.randomUUID();

            for (RateLimitAlgorithm algorithm : RateLimitAlgorithm.values()) {
                String key = namespace + ':' + algorithm.getConfigKey();
                RateLimitRule rule = rule(algorithm);
                RateLimitDecision first = executor.execute(rule, key).block(Duration.ofSeconds(5));
                RateLimitDecision second = executor.execute(rule, key).block(Duration.ofSeconds(5));

                assertThat(first).as("first request for %s", algorithm).isNotNull();
                assertThat(first.allowed()).as("first request for %s", algorithm).isTrue();
                assertThat(second).as("second request for %s", algorithm).isNotNull();
                assertThat(second.allowed()).as("second request for %s", algorithm).isFalse();
            }
        } finally {
            factory.destroy();
        }
    }

    private LettuceConnectionFactory createConnectionFactory(String mode) {
        LettuceClientConfiguration.LettuceClientConfigurationBuilder client =
                LettuceClientConfiguration.builder().commandTimeout(Duration.ofSeconds(5));
        if (Boolean.parseBoolean(System.getenv("PEACH_REDIS_SSL"))) {
            client.useSsl();
        }
        LettuceClientConfiguration clientConfiguration = client.build();
        String password = System.getenv("PEACH_REDIS_PASSWORD");

        return switch (mode.trim().toLowerCase()) {
            case "cluster" -> {
                String nodes = required("PEACH_REDIS_NODES");
                RedisClusterConfiguration configuration =
                        new RedisClusterConfiguration(List.of(nodes.split(",")));
                if (password != null && !password.isBlank()) {
                    configuration.setPassword(RedisPassword.of(password));
                }
                yield new LettuceConnectionFactory(configuration, clientConfiguration);
            }
            case "sentinel" -> {
                String master = required("PEACH_REDIS_SENTINEL_MASTER");
                String nodes = required("PEACH_REDIS_SENTINELS");
                RedisSentinelConfiguration configuration =
                        new RedisSentinelConfiguration(master, Set.of());
                for (String node : nodes.split(",")) {
                    String[] address = node.trim().split(":", 2);
                    if (address.length != 2) {
                        throw new IllegalArgumentException("Invalid PEACH_REDIS_SENTINELS entry: " + node);
                    }
                    configuration.sentinel(address[0].trim(), Integer.parseInt(address[1]));
                }
                if (password != null && !password.isBlank()) {
                    configuration.setPassword(RedisPassword.of(password));
                }
                String sentinelPassword = System.getenv("PEACH_REDIS_SENTINEL_PASSWORD");
                if (sentinelPassword != null && !sentinelPassword.isBlank()) {
                    configuration.setSentinelPassword(RedisPassword.of(sentinelPassword));
                }
                yield new LettuceConnectionFactory(configuration, clientConfiguration);
            }
            case "standalone" -> {
                RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(
                        System.getenv().getOrDefault("PEACH_REDIS_HOST", "127.0.0.1"),
                        Integer.parseInt(System.getenv().getOrDefault("PEACH_REDIS_PORT", "6379")));
                if (password != null && !password.isBlank()) {
                    configuration.setPassword(RedisPassword.of(password));
                }
                yield new LettuceConnectionFactory(configuration, clientConfiguration);
            }
            default -> throw new IllegalArgumentException("Unsupported PEACH_REDIS_MODE: " + mode);
        };
    }

    private String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Environment variable is required: " + name);
        }
        return value;
    }

    private RateLimitRule rule(RateLimitAlgorithm algorithm) {
        return new RateLimitRule(
                "topology-" + algorithm.getConfigKey(),
                true,
                RateLimitScope.GLOBAL,
                algorithm,
                1,
                Set.of(),
                List.of(),
                Set.of(),
                1,
                Duration.ofSeconds(60).toMillis(),
                0.000001D,
                1,
                0.000001D,
                1);
    }
}
