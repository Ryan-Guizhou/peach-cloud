package com.peach.redis.stream.quickstart;

import com.peach.redis.stream.MessageConsumer;
import com.peach.redis.stream.RedisStreamPendingRecovery;
import com.peach.redis.stream.config.RedisStreamProperties;
import com.peach.redis.stream.config.RedisStreamRecoveryProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 通过真实 Redis 7.2 验证消费组 PEL 恢复、异常保留与死信转移。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@EnabledIfEnvironmentVariable(named = "RUN_REDIS_CLIENT_IT", matches = "true")
class RedisStreamRecoveryServerIntegrationTest {

    private JedisConnectionFactory connectionFactory;
    private StringRedisTemplate redis;

    @BeforeEach
    void connect() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration("127.0.0.1", 6379);
        configuration.setDatabase(0);
        connectionFactory = new JedisConnectionFactory(configuration);
        connectionFactory.afterPropertiesSet();
        redis = new StringRedisTemplate(connectionFactory);
        redis.afterPropertiesSet();
    }

    @AfterEach
    void disconnect() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void abandonedPendingRecordIsClaimedAndAckedOnlyAfterSuccessfulCallback() throws InterruptedException {
        String stream = streamName();
        String group = "workers";
        try {
            RecordId original = preparePending(stream, group);
            AtomicReference<RecordId> deliveredId = new AtomicReference<>();
            AtomicReference<String> deliveredValue = new AtomicReference<>();
            MessageConsumer consumer = record -> {
                deliveredId.set(record.getId());
                deliveredValue.set(record.getValue());
            };

            Thread.sleep(100);
            RedisStreamPendingRecovery recovery = recovery(stream, group, 3, consumer);
            assertThat(recovery.recoverOnce()).isEqualTo(1);
            assertThat(deliveredId.get()).isEqualTo(original);
            assertThat(deliveredValue.get()).isEqualTo("order:paid");
            assertThat(redis.opsForStream().pending(stream, group).getTotalPendingMessages()).isZero();
        } finally {
            redis.delete(stream);
        }
    }

    @Test
    void callbackFailureRetainsPendingWithoutFalseAcknowledgment() throws InterruptedException {
        String stream = streamName();
        String group = "workers";
        try {
            RecordId sourceId = preparePending(stream, group);
            MessageConsumer consumer = record -> {
                throw new IllegalStateException("transient business failure");
            };

            Thread.sleep(100);
            RedisStreamPendingRecovery recovery = recovery(stream, group, 3, consumer);
            assertThat(recovery.recoverOnce()).isZero();
            assertThat(redis.opsForStream().pending(stream, group).getTotalPendingMessages()).isEqualTo(1L);
            assertThat(Boolean.TRUE.equals(redis.hasKey(stream + ":dlq"))).isFalse();

            // 模拟 DLQ XADD 已持久化但进程在源 XACK 前崩溃：只读对账发现 PEL。
            RecordId deadLetterId = redis.opsForStream().add(StreamRecords.newRecord()
                    .in(stream + ":dlq").ofMap(java.util.Map.of(
                            "sourceStream", stream, "sourceGroup", group,
                            "sourceId", sourceId.getValue(), "payload", "{_raw=order:paid}")));
            assertThat(deadLetterId).isNotNull();
            RedisStreamPendingRecovery.DeadLetterTransferState before =
                    recovery.deadLetterTransferState(deadLetterId.getValue());
            assertThat(before.sourceId()).isEqualTo(sourceId.getValue());
            assertThat(before.pending()).isTrue();
            assertThat(before.sourceAvailable()).isTrue();
            assertThat(redis.opsForStream().pending(stream, group).getTotalPendingMessages()).isEqualTo(1L);

            // 模拟运维在额外业务对账后确认源消息，状态查询本身绝不执行 XACK。
            redis.opsForStream().acknowledge(stream, group, sourceId);
            RedisStreamPendingRecovery.DeadLetterTransferState after =
                    recovery.deadLetterTransferState(deadLetterId.getValue());
            assertThat(after.pending()).isFalse();
            assertThat(after.sourceAvailable()).isTrue();
        } finally {
            redis.delete(stream);
            redis.delete(stream + ":dlq");
        }
    }

    @Test
    void exhaustedDeliveryIsStoredInDlqBeforeSourceAck() throws InterruptedException {
        String stream = streamName();
        String group = "workers";
        String dlq = stream + ":dlq";
        try {
            RecordId original = preparePending(stream, group);
            MessageConsumer consumer = record -> {
                throw new AssertionError("Exhausted message must not invoke business callback");
            };

            Thread.sleep(100);
            assertThat(recovery(stream, group, 1, consumer).recoverOnce()).isEqualTo(1);
            assertThat(redis.opsForStream().pending(stream, group).getTotalPendingMessages()).isZero();

            List<MapRecord<String, Object, Object>> records =
                    redis.opsForStream().range(dlq, Range.unbounded());
            assertThat(records).hasSize(1);
            assertThat(records.get(0).getValue().get("sourceId")).isEqualTo(original.getValue());
            assertThat(records.get(0).getValue().get("sourceStream")).isEqualTo(stream);
            assertThat(records.get(0).getValue().get("sourceGroup")).isEqualTo(group);
        } finally {
            redis.delete(stream);
            redis.delete(dlq);
        }
    }

    @Test
    void operatorCanInspectAndReplayDeadLetterWithoutLosingOriginalFields() throws InterruptedException {
        String stream = streamName();
        String group = "workers";
        String dlq = stream + ":dlq";
        StreamOperations<String, Object, Object> operations = redis.opsForStream();
        try {
            RecordId sourceId = preparePending(stream, group);
            RedisStreamPendingRecovery recovery = recovery(stream, group, 1, record -> {
                throw new AssertionError("Exhausted message must not execute");
            });
            assertThat(recovery.backlog().pending()).isEqualTo(1);
            assertThat(recovery.backlog().deadLetters()).isZero();

            Thread.sleep(100);
            assertThat(recovery.recoverOnce()).isEqualTo(1);
            assertThat(recovery.backlog().pending()).isZero();
            assertThat(recovery.backlog().deadLetters()).isEqualTo(1);

            List<MapRecord<String, Object, Object>> deadLetters =
                    operations.range(dlq, Range.unbounded());
            assertThat(deadLetters).hasSize(1);
            String deadLetterId = deadLetters.get(0).getId().getValue();
            RecordId publishedId = recovery.replayDeadLetter(deadLetterId);
            assertThat(publishedId).isNotEqualTo(sourceId);

            List<MapRecord<String, Object, Object>> original =
                    operations.range(stream, Range.closed(sourceId.getValue(), sourceId.getValue()));
            List<MapRecord<String, Object, Object>> replayed =
                    operations.range(stream, Range.closed(publishedId.getValue(), publishedId.getValue()));
            assertThat(replayed).hasSize(1);
            assertThat(replayed.get(0).getValue()).isEqualTo(original.get(0).getValue());
            assertThat(recovery.backlog().deadLetters()).isEqualTo(1);

            // 新死信自带精确原始字段；即使旧源 entry 已被裁剪也能重放。
            operations.delete(stream, sourceId);
            RecordId afterTrim = recovery.replayDeadLetter(deadLetterId);
            List<MapRecord<String, Object, Object>> recovered =
                    operations.range(stream, Range.closed(afterTrim.getValue(), afterTrim.getValue()));
            assertThat(recovered).hasSize(1);
            assertThat(recovered.get(0).getValue()).isEqualTo(original.get(0).getValue());

            // 历史 DLQ 没有结构化字段时，仍只允许精确查询旧源；找不到必须报错。
            RecordId oldDeadLetterId = operations.add(StreamRecords.newRecord().in(dlq).ofMap(
                    java.util.Map.of("sourceStream", stream, "sourceGroup", group,
                            "sourceId", sourceId.getValue(), "payload", "{_raw=old}")));
            assertThat(oldDeadLetterId).isNotNull();
            assertThatThrownBy(() -> recovery.replayDeadLetter(oldDeadLetterId.getValue()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("trimmed");
        } finally {
            redis.delete(stream);
            redis.delete(dlq);
        }
    }

    private RecordId preparePending(String stream, String group) {
        StreamOperations<String, Object, Object> ops = redis.opsForStream();
        RecordId id = ops.add(StreamRecords.newRecord().in(stream).ofObject("order:paid"));
        assertThat(id).isNotNull();
        ops.createGroup(stream, ReadOffset.from("0"), group);
        List<MapRecord<String, Object, Object>> delivered = ops.read(
                Consumer.from(group, "abandoned-consumer"),
                StreamReadOptions.empty().count(1),
                StreamOffset.create(stream, ReadOffset.lastConsumed()));
        assertThat(delivered).hasSize(1);
        assertThat(delivered.get(0).getId()).isEqualTo(id);
        assertThat(ops.pending(stream, group).getTotalPendingMessages()).isEqualTo(1L);
        return id;
    }

    private RedisStreamPendingRecovery recovery(String stream, String group, int maxAttempts,
                                               MessageConsumer consumer) {
        RedisStreamProperties streamProperties = new RedisStreamProperties();
        streamProperties.setStreamName(stream);
        streamProperties.setConsumerGroup(group);
        streamProperties.setConsumerName("recoverer");

        RedisStreamRecoveryProperties recoveryProperties = new RedisStreamRecoveryProperties();
        recoveryProperties.setEnabled(true);
        recoveryProperties.setBatchSize(8);
        recoveryProperties.setMinIdle(Duration.ofMillis(10));
        recoveryProperties.setMaxRetryDelay(Duration.ofMillis(200));
        recoveryProperties.setMaxAttempts(maxAttempts);
        recoveryProperties.setInterval(Duration.ofSeconds(1));
        return new RedisStreamPendingRecovery(redis, streamProperties, recoveryProperties, consumer);
    }

    private static String streamName() {
        return "stream-recovery:it:" + UUID.randomUUID();
    }
}
