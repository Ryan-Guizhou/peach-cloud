package com.peach.redisson.quickstart;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redisson-only Starter 的真实 Redis 连通性与客户端隔离验证。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@SpringBootTest(properties = "quickstart.redisson.demo.enabled=false")
@Testcontainers(disabledWithoutDocker = true)
class RedissonOnlyCapabilityTest {
    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7.2-alpine")).withExposedPorts(6379);

    @DynamicPropertySource
    static void redisConfig(DynamicPropertyRegistry registry) {
        registry.add("peach.redis.host",
                () -> REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
        registry.add("peach.redis.password", () -> "");
        registry.add("peach.redis.database", () -> "0");
    }

    @Autowired
    ApplicationContext context;

    @Autowired
    RedissonClient redissonClient;

    @Test
    void redissonOnlyStarterShouldNotCreateSpringDataRedisOrJedisBeans() {
        assertThat(context.getBeansOfType(RedissonClient.class)).hasSize(1);
        assertThat(context.containsBean("jedisConnectionFactory")).isFalse();
        assertThat(context.containsBean("redisTemplate")).isFalse();

        String key = "quickstart:redisson-only:test";
        try {
            redissonClient.getBucket(key).set("ok");
            assertThat(redissonClient.getBucket(key).get()).isEqualTo("ok");
        } finally {
            redissonClient.getBucket(key).delete();
        }
    }
}
