package com.peach.redis.stream;

import com.peach.redis.stream.config.RedisStreamProperties;
import com.peach.redis.stream.config.RedisStreamRecoveryProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Stream Consumer Group Pending 任务恢复器。
 *
 * <p>通过 XPENDING + XCLAIM 从其他实例的未确认记录中认领超时任务。
 * 原业务回调成功才 XACK；持续失败时写入 DLQ 后再 ACK。业务回调可能
 * 在 ACK 丢失后重复执行，因此业务侧持久化幂等是必要条件。</p>
 *
 * <p>DLQ XADD 与原消息 XACK 非跨键事务，崩溃可产生重复死信但不会
 * 主动确认尚未成功写入 DLQ 的记录。不得声称 exactly-once。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@Slf4j
public final class RedisStreamPendingRecovery {

    private final StringRedisTemplate redis;
    private final RedisStreamProperties streamProperties;
    private final RedisStreamRecoveryProperties recoveryProperties;
    private final MessageConsumer messageConsumer;
    private final String recoveryConsumer;
    private ScheduledExecutorService scheduler;
    // XPENDING range 轮转，避免前 N 条不可认领记录永久饿死后续消息。
    private String pendingCursor;

    public RedisStreamPendingRecovery(StringRedisTemplate redis,
                                      RedisStreamProperties streamProperties,
                                      RedisStreamRecoveryProperties recoveryProperties,
                                      MessageConsumer messageConsumer) {
        this.redis = Objects.requireNonNull(redis, "redis");
        this.streamProperties = Objects.requireNonNull(streamProperties, "streamProperties");
        this.recoveryProperties = Objects.requireNonNull(recoveryProperties, "recoveryProperties");
        this.messageConsumer = Objects.requireNonNull(messageConsumer, "messageConsumer");
        this.recoveryConsumer = streamProperties.getConsumerName() + "-recovery-"
                + UUID.randomUUID().toString().substring(0, 8);
        recoveryProperties.validate();
    }

    /** Bean 生命周期入口：只在显式开启 recovery 时启动。 */
    public synchronized void start() {
        if (!recoveryProperties.isEnabled() || scheduler != null) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "peach-redis-stream-recovery");
            thread.setDaemon(true);
            return thread;
        });
        long interval = recoveryProperties.getInterval().toMillis();
        scheduler.scheduleWithFixedDelay(this::recoverSafely, interval, interval, TimeUnit.MILLISECONDS);
        log.info("Redis Stream Pending recovery started, stream={}, group={}",
                streamProperties.getStreamName(), streamProperties.getConsumerGroup());
    }

    /** 停止扫描并终止私有线程池；不清除 Pending 消息。 */
    public void stop() {
        ScheduledExecutorService executor;
        synchronized (this) {
            executor = scheduler;
            scheduler = null;
        }
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException exception) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void recoverSafely() {
        try {
            recoverOnce();
        } catch (Exception exception) {
            log.error("Redis Stream Pending sweep failed, stream={}, group={}",
                    streamProperties.getStreamName(), streamProperties.getConsumerGroup(), exception);
        }
    }

    /**
     * 执行一个有界扫描批次。提供给运维测试使用，连续调用不得绕过 minIdle。
     *
     * @return 已确认成功或移入死信的记录数量
     */
    public synchronized int recoverOnce() {
        String stream = streamProperties.getStreamName();
        String group = streamProperties.getConsumerGroup();
        StreamOperations<String, Object, Object> operations = redis.opsForStream();
        Range<String> range = pendingCursor == null
                ? Range.unbounded()
                : Range.from(Range.Bound.exclusive(pendingCursor)).to(Range.Bound.unbounded());
        PendingMessages pending = operations.pending(stream, group, range, recoveryProperties.getBatchSize());
        if (pending == null || pending.isEmpty()) {
            pendingCursor = null;
            return 0;
        }
        pendingCursor = pending.size() == recoveryProperties.getBatchSize()
                ? pending.get(pending.size() - 1).getIdAsString() : null;

        int completed = 0;
        for (PendingMessage entry : pending) {
            Duration requiredIdle = minIdleForAttempt(entry.getTotalDeliveryCount());
            if (entry.getElapsedTimeSinceLastDelivery().compareTo(requiredIdle) < 0) {
                continue;
            }
            List<MapRecord<String, Object, Object>> claimed = operations.claim(
                    stream, group, recoveryConsumer, requiredIdle, entry.getId());
            if (claimed == null || claimed.isEmpty()) {
                continue; // Another instance claimed the record, or it was removed.
            }
            for (MapRecord<String, Object, Object> raw : claimed) {
                if (entry.getTotalDeliveryCount() >= recoveryProperties.getMaxAttempts()) {
                    try {
                        sendDeadLetter(operations, raw, entry);
                        acknowledge(operations, stream, group, raw.getId());
                        completed++;
                    } catch (Exception exception) {
                        log.error("Redis Stream DLQ transfer failed; message remains pending, id={}",
                                raw.getId(), exception);
                    }
                    continue;
                }
                try {
                    ObjectRecord<String, String> message = operations.map(raw, String.class);
                    messageConsumer.accept(message);
                    acknowledge(operations, stream, group, raw.getId());
                    completed++;
                } catch (Exception exception) {
                    log.warn("Redis Stream Pending delivery failed; retained for retry, id={}, attempt={}",
                            raw.getId(), entry.getTotalDeliveryCount() + 1, exception);
                }
            }
        }
        return completed;
    }

    private Duration minIdleForAttempt(long deliveries) {
        long baseMillis = recoveryProperties.getMinIdle().toMillis();
        long capMillis = recoveryProperties.getMaxRetryDelay().toMillis();
        int exponent = (int) Math.min(20, Math.max(0, deliveries - 1));
        long factor = 1L << exponent;
        if (baseMillis > capMillis / factor) {
            return Duration.ofMillis(capMillis);
        }
        return Duration.ofMillis(baseMillis * factor);
    }

    private void sendDeadLetter(StreamOperations<String, Object, Object> ops,
                                MapRecord<String, Object, Object> record,
                                PendingMessage pending) {
        String stream = streamProperties.getStreamName();
        String dlq = recoveryProperties.getDeadLetterStream();
        if (dlq == null || dlq.isBlank()) {
            dlq = stream + ":dlq";
        }
        if (stream.equals(dlq)) {
            throw new IllegalArgumentException("Dead-letter stream must differ from source stream");
        }
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("sourceStream", stream);
        fields.put("sourceGroup", streamProperties.getConsumerGroup());
        fields.put("sourceId", record.getId().getValue());
        fields.put("attempts", Long.toString(pending.getTotalDeliveryCount()));
        fields.put("payload", String.valueOf(record.getValue()));
        RecordId dlqId = ops.add(StreamRecords.newRecord().in(dlq).ofMap(fields));
        if (dlqId == null) {
            throw new IllegalStateException("Redis DLQ XADD returned null");
        }
        log.warn("Redis Stream dead letter persisted, sourceId={}, dlqId={}", record.getId(), dlqId);
    }

    private static void acknowledge(StreamOperations<String, Object, Object> ops,
                                    String stream, String group, RecordId id) {
        Long acknowledged = ops.acknowledge(stream, group, id);
        if (acknowledged == null || acknowledged != 1L) {
            throw new IllegalStateException("Redis Stream XACK returned no confirmation for " + id);
        }
    }
}
