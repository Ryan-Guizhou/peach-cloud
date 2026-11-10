package com.peach.redis.quickstart;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 使用独立 Redis 7.2 服务容器验证 Redis-only Starter 真实读写和客户端隔离。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@EnabledIfEnvironmentVariable(named = "RUN_REDIS_CLIENT_IT", matches = "true")
@SpringBootTest(properties = {
        "peach.redis.mode=standalone",
        "peach.redis.host=127.0.0.1:6379",
        "peach.redis.database=0"
})
class RedisOnlyServerIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private StringRedisTemplate redis;

    @Test
    void redisOnlyStarterShouldPersistWithoutCreatingRedisson() {
        assertThat(context.getBeansOfType(RedisConnectionFactory.class)).hasSize(1);
        assertThat(context.getBean(RedisConnectionFactory.class)).isInstanceOf(JedisConnectionFactory.class);
        assertThat(context.containsBean("redissonClient")).isFalse();
        String key = "redis-only:it:" + UUID.randomUUID();
        try {
            redis.opsForValue().set(key, "ready", Duration.ofSeconds(30));
            assertThat(redis.opsForValue().get(key)).isEqualTo("ready");
            assertThat(redis.getExpire(key)).isGreaterThan(0);
        } finally {
            redis.delete(key);
        }
    }
}
