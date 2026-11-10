package com.peach.redisson.quickstart;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 使用独立 Redis 7.2 服务容器验证 Redisson-only Starter 不生成 Jedis Bean。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@EnabledIfEnvironmentVariable(named = "RUN_REDIS_CLIENT_IT", matches = "true")
@SpringBootTest(properties = {
        "peach.redis.mode=standalone",
        "peach.redis.host=127.0.0.1:6379",
        "peach.redis.database=0",
        "quickstart.redisson.demo.enabled=false"
})
class RedissonOnlyServerIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private RedissonClient redisson;

    @Test
    void redissonOnlyStarterShouldPersistWithoutCreatingRedisTemplate() {
        assertThat(context.getBeansOfType(RedissonClient.class)).hasSize(1);
        assertThat(context.containsBean("jedisConnectionFactory")).isFalse();
        assertThat(context.containsBean("redisTemplate")).isFalse();
        String key = "redisson-only:it:" + UUID.randomUUID();
        try {
            redisson.getBucket(key).set("ready");
            assertThat(redisson.getBucket(key).get()).isEqualTo("ready");
        } finally {
            redisson.getBucket(key).delete();
        }
    }
}
