package com.peach.redission.delayqueue.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 当前唯一可靠延迟队列的运行参数。
 *
 * <p>沿用 peach.delay.queue 配置域，使用同槽 Lua 原子认领、租约与 fencing。
 * 已有 Redisson delayed-queue 消息必须在发布切换前排空，不会自动消费旧协议。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@Data
@ConfigurationProperties(prefix = DelayQueueProperties.PREFIX)
public class DelayQueueProperties {

    public static final String PREFIX = "peach.delay.queue";

    private boolean enabled = true;
    private int workerCount = 4;
    private int isolationRegionCount = 5;
    private int maxTopicSubscriptions = 512;
    private int batchSize = 16;
    private int maxAttempts = 5;
    private Duration pollInterval = Duration.ofMillis(500);
    private Duration lease = Duration.ofMinutes(2);
    private Duration retryInitial = Duration.ofSeconds(5);
    private Duration retryMax = Duration.ofMinutes(5);
    private Duration completedRetention = Duration.ofDays(7);
    private int maxPayloadBytes = 65536;

    /**
     * 在初始化客户端及任务订阅之前校验资源和时间边界。
     */
    public void validate() {
        if (workerCount < 1 || workerCount > 64
                || isolationRegionCount < 1 || isolationRegionCount > 100
                || maxTopicSubscriptions < isolationRegionCount || maxTopicSubscriptions > 10000
                || batchSize < 1 || batchSize > 512
                || maxAttempts < 1 || maxAttempts > 1000
                || maxPayloadBytes < 1 || maxPayloadBytes > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Invalid delay queue capacity settings");
        }
        if (pollInterval == null || pollInterval.toMillis() < 1
                || lease == null || lease.toMillis() < 3000
                || retryInitial == null || retryInitial.toMillis() < 1
                || retryMax == null || retryMax.compareTo(retryInitial) < 0
                || completedRetention == null || completedRetention.toMillis() < 1) {
            throw new IllegalArgumentException("Invalid delay queue duration settings");
        }
    }
}
