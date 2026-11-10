package com.peach.redisson.quickstart;

import org.redisson.api.RedissonClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 演示仅通过 RedissonClient 操作 Redis Bucket。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@Component
@ConditionalOnProperty(prefix = "quickstart.redisson.demo", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class RedissonOnlyDemoRunner implements ApplicationRunner {

    private final RedissonClient redissonClient;

    public RedissonOnlyDemoRunner(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public void run(ApplicationArguments args) {
        String key = "quickstart:redisson-only:sample";
        try {
            redissonClient.getBucket(key).set("peach-redisson-ready");
            String result = (String) redissonClient.getBucket(key).get();
            if (!"peach-redisson-ready".equals(result)) {
                throw new IllegalStateException("Redisson-only roundtrip failed");
            }
        } finally {
            redissonClient.getBucket(key).delete();
        }
    }
}
