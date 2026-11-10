package com.peach.redis.quickstart;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis-only Starter 在真实 Redis 上的独立启动验证。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@SpringBootTest(properties = "quickstart.redis.demo.enabled=false")
@Testcontainers(disabledWithoutDocker = true)
class RedisOnlyCapabilityTest {
    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7.2-alpine")).withExposedPorts(6379);

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry properties) {
        properties.add("peach.redis.host",
                () -> REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
        properties.add("peach.redis.password", () -> "");
        properties.add("peach.redis.database", () -> "0");
    }

    @Autowired
    private ApplicationContext context;

    @Autowired
    private StringRedisTemplate redis;

    @Test
    void redisOnlyStarterShouldUseJedisAndNotCreateRedisson() {
        assertThat(context.getBeansOfType(RedisConnectionFactory.class)).hasSize(1);
        assertThat(context.getBean(RedisConnectionFactory.class))
                .isInstanceOf(JedisConnectionFactory.class);
        assertThat(context.containsBean("redissonClient")).isFalse();
        String key = "quickstart:redis-only:test";
        try {
            redis.opsForValue().set(key, "ok", Duration.ofSeconds(30));
            assertThat(redis.opsForValue().get(key)).isEqualTo("ok");
        } finally {
            redis.delete(key);
        }
    }
}
