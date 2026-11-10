package com.peach.redission.delayqueue.v2;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 延迟队列 V2 可选协议参数。旧 RDelayedQueue 不会被自动迁移。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@Data
@ConfigurationProperties(prefix = "peach.delay.queue.v2")
public class DelayQueueV2Properties {

    private boolean enabled = false;
    private int workerCount = 4;
    private int batchSize = 16;
    private int maxAttempts = 5;
    private Duration pollInterval = Duration.ofMillis(500);
    private Duration lease = Duration.ofMinutes(2);
    private Duration retryInitial = Duration.ofSeconds(5);
    private Duration retryMax = Duration.ofMinutes(5);
    private Duration completedRetention = Duration.ofDays(7);
    private int maxPayloadBytes = 65536;

    /**
     * 配置要在启动消费者之前完成边界校验，避免无限队列和异常重试配置。
     */
    public void validate() {
        if (workerCount < 1 || workerCount > 64
                || batchSize < 1 || batchSize > 512
                || maxAttempts < 1 || maxAttempts > 1000
                || maxPayloadBytes < 1 || maxPayloadBytes > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Invalid delay queue V2 capacity bounds");
        }
        if (pollInterval == null || pollInterval.toMillis() < 1
                || lease == null || lease.toMillis() < 3000
                || retryInitial == null || retryInitial.toMillis() < 1
                || retryMax == null || retryMax.compareTo(retryInitial) < 0
                || completedRetention == null || completedRetention.toMillis() < 1) {
            throw new IllegalArgumentException("Invalid delay queue V2 durations");
        }
    }
}
