package com.peach.redis.quickstart;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 独立 Redis Starter 的 KV / TTL 最小运行示例。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@Component
@ConditionalOnProperty(prefix = "quickstart.redis.demo", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class RedisOnlyDemoRunner implements ApplicationRunner {

    private final StringRedisTemplate redis;

    public RedisOnlyDemoRunner(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void run(ApplicationArguments args) {
        String key = "quickstart:redis-only:sample";
        try {
            redis.opsForValue().set(key, "peach-redis-ready", Duration.ofSeconds(30));
            if (!"peach-redis-ready".equals(redis.opsForValue().get(key))) {
                throw new IllegalStateException("Redis-only roundtrip failed");
            }
        } finally {
            redis.delete(key);
        }
    }
}
