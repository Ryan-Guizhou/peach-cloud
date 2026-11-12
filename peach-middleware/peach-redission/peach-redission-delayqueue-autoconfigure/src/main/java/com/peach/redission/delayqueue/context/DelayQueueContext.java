package com.peach.redission.delayqueue.context;

import com.peach.redission.delayqueue.autoconfigure.DelayQueueProperties;
import com.peach.redission.delayqueue.core.RedisDelayQueue;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 延迟队列业务生产入口，所有消息均写入统一可靠队列。
 *
 * <p>旧 sendMessage 签名保持不变；需要跨请求去重的业务应传入稳定 messageId。
 * 同一 messageId 固定映射到同一隔离分区，避免并发生产时重复 ID 跨分区。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
public class DelayQueueContext {

    private final RedisDelayQueue queue;
    private final DelayQueueProperties properties;

    /** 对每个业务 topic 在首次入队时检查所有旧分区，不做 Redis 全局扫描。 */
    private final Map<String, Boolean> migrationChecked = new ConcurrentHashMap<>();

    public DelayQueueContext(RedisDelayQueue queue, DelayQueueProperties properties) {
        this.queue = Objects.requireNonNull(queue, "queue");
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    /**
     * 兼容既有生产入口，为没有业务 ID 的消息分配唯一 ID。
     */
    public void sendMessage(String topic, String content, long delayTime, TimeUnit timeUnit) {
        boolean accepted = sendMessage(topic, UUID.randomUUID().toString(), content, delayTime, timeUnit);
        if (!accepted) {
            throw new IllegalStateException("Generated delay queue message ID unexpectedly duplicated");
        }
    }

    /**
     * 使用稳定业务消息 ID 入队；重复 ID 在同一分区内不会重复入队。
     *
     * @return 首次接受返回 true，已存在返回 false
     */
    public boolean sendMessage(String topic, String messageId, String content,
                               long delayTime, TimeUnit timeUnit) {
        Objects.requireNonNull(timeUnit, "timeUnit");
        if (delayTime < 0) {
            throw new IllegalArgumentException("delayTime must be nonnegative");
        }
        Objects.requireNonNull(topic, "topic");
        migrationChecked.computeIfAbsent(topic, name -> {
            for (int i = 0; i < properties.getIsolationRegionCount(); i++) {
                queue.assertLegacyDrained(name + "-" + i);
            }
            return Boolean.TRUE;
        });
        int shard = Math.floorMod(Objects.requireNonNull(messageId, "messageId").hashCode(),
                properties.getIsolationRegionCount());
        String partition = topic + "-" + shard;
        return queue.offer(partition, messageId, content,
                Duration.ofMillis(timeUnit.toMillis(delayTime)));
    }
}
