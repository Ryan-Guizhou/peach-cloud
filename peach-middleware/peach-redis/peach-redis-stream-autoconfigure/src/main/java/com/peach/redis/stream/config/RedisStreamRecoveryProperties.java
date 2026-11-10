package com.peach.redis.stream.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Redis Stream Pending 恢复配置。默认禁用，启用前必须保证业务消费者具备幂等性。
 * 一个批次最多处理 batchSize 条记录，故障时下一轮继续。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@Data
@ConfigurationProperties(prefix = "peach.redis.stream.recovery")
public class RedisStreamRecoveryProperties {

    private boolean enabled = false;
    private int batchSize = 32;
    private int maxAttempts = 5;
    private Duration minIdle = Duration.ofSeconds(60);
    private Duration maxRetryDelay = Duration.ofMinutes(15);
    private Duration interval = Duration.ofSeconds(30);
    private String deadLetterStream;

    /**
     * 对配置边界做 fail-fast 校验，避免 0 间隔或无限批量扫描。
     */
    public void validate() {
        if (batchSize < 1 || batchSize > 1000) {
            throw new IllegalArgumentException("Redis stream recovery batchSize must be 1..1000");
        }
        if (maxAttempts < 1 || maxAttempts > 1000) {
            throw new IllegalArgumentException("Redis stream recovery maxAttempts must be 1..1000");
        }
        if (minIdle == null || minIdle.toMillis() < 1
                || maxRetryDelay == null || maxRetryDelay.compareTo(minIdle) < 0
                || interval == null || interval.toMillis() < 1) {
            throw new IllegalArgumentException("Invalid Redis stream recovery durations");
        }
    }
}
