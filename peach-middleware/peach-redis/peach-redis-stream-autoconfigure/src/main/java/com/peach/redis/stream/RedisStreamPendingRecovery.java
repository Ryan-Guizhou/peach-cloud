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

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
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

    private static final String ORIGINAL_FIELD_PREFIX = "originalField:";
    private static final String ORIGINAL_FIELDS_MARKER = "originalFieldsStored";

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

    /**
     * 只读取当前消费组的 PEL 数量和死信 Stream 条数，不扫描具体消息或读取 payload。
     *
     * @return 当前待确认和死信的实时计数；Redis 读取失败时向调用方抛出异常
     */
    public StreamBacklog backlog() {
        String stream = streamProperties.getStreamName();
        String group = streamProperties.getConsumerGroup();
        StreamOperations<String, Object, Object> ops = redis.opsForStream();
        var pending = ops.pending(stream, group);
        if (pending == null) {
            throw new IllegalStateException("Redis Stream Pending summary is unavailable");
        }
        Long dlqCount = ops.size(deadLetterStream());
        if (dlqCount == null) {
            throw new IllegalStateException("Redis Stream DLQ count is unavailable");
        }
        return new StreamBacklog(pending.getTotalPendingMessages(), dlqCount);
    }

    /**
     * 运维手动重放死信记录。
     *
     * <p>只允许重放本组件配置的源 Stream + Group；从原 Stream 精确读取原始字段，
     * 避免将历史死信里用于展示的 Map 字符串作为业务消息内容。原记录已被裁剪时拒绝重放。
     * 成功后保留 DLQ 审计记录；重复调用可能重复投递，业务必须依赖持久幂等。</p>
     *
     * @param deadLetterRecordId 死信 Stream 中的 Redis RecordId，例如 123456789-0
     * @return 原 Stream 中新写入的 RecordId；写入失败时不修改死信记录
     */
    public synchronized RecordId replayDeadLetter(String deadLetterRecordId) {
        String stream = streamProperties.getStreamName();
        StreamOperations<String, Object, Object> ops = redis.opsForStream();
        DeadLetterReference reference = readDeadLetterReference(ops, deadLetterRecordId);
        String id = reference.sourceId();
        Map<String, Object> metadata = reference.metadata();
        Map<Object, Object> originalFields = readOriginalFields(metadata);
        if (originalFields == null) {
            // 兼容历史 DLQ：旧记录只存展示用 payload，必须从原 Stream 精确读取。
            List<MapRecord<String, Object, Object>> originals =
                    ops.range(stream, Range.closed(id, id));
            if (originals == null || originals.size() != 1) {
                throw new IllegalStateException("Redis Stream original record has been trimmed; cannot safely replay");
            }
            originalFields = new LinkedHashMap<>(originals.get(0).getValue());
        }

        RecordId replayed = ops.add(StreamRecords.newRecord().in(stream).ofMap(originalFields));
        if (replayed == null) {
            throw new IllegalStateException("Redis Stream replay XADD returned null");
        }
        log.info("Redis Stream dead letter replayed, originalId={}, replayedId={}", id, replayed);
        return replayed;
    }

    /**
     * 只读判定“死信已保存但源消息可能尚未 XACK”的故障窗口。
     * 精确读取一个 DLQ、一个源 Stream ID 和至多一个 Pending 项，不进行全量扫描。
     *
     * <p>返回值仅是瞬时快照，不能据此自动 XACK 或推断业务事务已完成。
     * 运维核对前必须先停止所有实例对该组的恢复和消费。</p>
     */
    public DeadLetterTransferState deadLetterTransferState(String deadLetterRecordId) {
        StreamOperations<String, Object, Object> ops = redis.opsForStream();
        DeadLetterReference reference = readDeadLetterReference(ops, deadLetterRecordId);
        String stream = streamProperties.getStreamName();
        String group = streamProperties.getConsumerGroup();
        String id = reference.sourceId();

        PendingMessages pending = ops.pending(stream, group, Range.closed(id, id), 1L);
        if (pending == null) {
            throw new IllegalStateException("Redis Stream Pending reconciliation is unavailable");
        }
        List<MapRecord<String, Object, Object>> originals = ops.range(stream, Range.closed(id, id));
        if (originals == null) {
            throw new IllegalStateException("Redis Stream source reconciliation is unavailable");
        }
        return new DeadLetterTransferState(id, !pending.isEmpty(), !originals.isEmpty());
    }

    /**
     * 死信与源组关系的只读快照，不涉及原业务载荷。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/11/10
     */
    public record DeadLetterTransferState(String sourceId, boolean pending, boolean sourceAvailable) {
    }

    private record DeadLetterReference(String sourceId, Map<String, Object> metadata) {
    }

    /** 统一校验人工重放与只读对账的源 Stream / Group / ID。 */
    private DeadLetterReference readDeadLetterReference(StreamOperations<String, Object, Object> ops,
                                                        String deadLetterRecordId) {
        if (deadLetterRecordId == null || !deadLetterRecordId.matches("[0-9]+-[0-9]+")) {
            throw new IllegalArgumentException("Invalid Redis Stream dead-letter record ID");
        }
        List<MapRecord<String, Object, Object>> deadLetters = ops.range(
                deadLetterStream(), Range.closed(deadLetterRecordId, deadLetterRecordId));
        if (deadLetters == null || deadLetters.size() != 1) {
            throw new IllegalStateException("Redis Stream dead-letter record was not found");
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        deadLetters.get(0).getValue().forEach((key, value) ->
                metadata.put(String.valueOf(key), value));
        if (!streamProperties.getStreamName().equals(metadata.get("sourceStream"))
                || !streamProperties.getConsumerGroup().equals(metadata.get("sourceGroup"))) {
            throw new IllegalStateException("Redis Stream dead-letter source does not match configured group");
        }
        Object sourceId = metadata.get("sourceId");
        if (!(sourceId instanceof String id) || !id.matches("[0-9]+-[0-9]+")) {
            throw new IllegalStateException("Redis Stream dead-letter source ID is invalid");
        }
        return new DeadLetterReference(id, metadata);
    }

    /**
     * 新写入的 DLQ 将业务字段单独保存，源 Stream 被裁剪后仍可原样重放。
     * 历史格式没有格式标记时返回 null，调用方仅从原始 Stream 读取。
     */
    private static Map<Object, Object> readOriginalFields(Map<String, Object> deadLetter) {
        Object marker = deadLetter.get(ORIGINAL_FIELDS_MARKER);
        if (marker == null) {
            return null;
        }
        if (!"true".equals(marker)) {
            throw new IllegalStateException("Redis Stream dead-letter original field marker is invalid");
        }
        Map<Object, Object> result = new LinkedHashMap<>();
        deadLetter.forEach((key, value) -> {
            if (!key.startsWith(ORIGINAL_FIELD_PREFIX)) {
                return;
            }
            String encodedKey = key.substring(ORIGINAL_FIELD_PREFIX.length());
            String originalKey;
            try {
                originalKey = new String(Base64.getUrlDecoder().decode(encodedKey), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException("Redis Stream dead-letter field name is invalid", exception);
            }
            if (originalKey.isBlank() || !encodeOriginalField(originalKey).equals(encodedKey)
                    || !(value instanceof String)) {
                throw new IllegalStateException("Redis Stream dead-letter original field is invalid");
            }
            if (result.put(originalKey, value) != null) {
                throw new IllegalStateException("Redis Stream dead-letter contains duplicate original field");
            }
        });
        if (result.isEmpty()) {
            throw new IllegalStateException("Redis Stream dead-letter original fields are missing");
        }
        return result;
    }

    private static String encodeOriginalField(String name) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(name.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 面向运维的轻量积压统计，不包含业务 payload。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/11/10 17:54
     */
    public record StreamBacklog(long pending, long deadLetters) {
    }

    private String deadLetterStream() {
        String source = streamProperties.getStreamName();
        String dlq = recoveryProperties.getDeadLetterStream();
        if (dlq == null || dlq.isBlank()) {
            dlq = source + ":dlq";
        }
        if (source.equals(dlq)) {
            throw new IllegalArgumentException("Dead-letter stream must differ from source stream");
        }
        return dlq;
    }

    private void sendDeadLetter(StreamOperations<String, Object, Object> ops,
                                MapRecord<String, Object, Object> record,
                                PendingMessage pending) {
        String stream = streamProperties.getStreamName();
        String dlq = deadLetterStream();
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("sourceStream", stream);
        fields.put("sourceGroup", streamProperties.getConsumerGroup());
        fields.put("sourceId", record.getId().getValue());
        fields.put("attempts", Long.toString(pending.getTotalDeliveryCount()));
        fields.put("payload", String.valueOf(record.getValue()));
        // 增量写入结构化原始业务字段；原五个字段保持不变，供旧运维工具读取。
        Map<Object, Object> original = record.getValue();
        if (original == null || original.isEmpty()) {
            throw new IllegalStateException("Redis Stream DLQ original fields are missing");
        }
        for (Map.Entry<Object, Object> item : original.entrySet()) {
            if (!(item.getKey() instanceof String name) || name.isBlank()
                    || !(item.getValue() instanceof String value)) {
                throw new IllegalStateException("Redis Stream DLQ requires String field names and values");
            }
            fields.put(ORIGINAL_FIELD_PREFIX + encodeOriginalField(name), value);
        }
        fields.put(ORIGINAL_FIELDS_MARKER, "true");
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
