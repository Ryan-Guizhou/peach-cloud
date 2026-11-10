package com.peach.redis.stream;

import com.peach.redis.stream.config.RedisStreamProperties;
import com.peach.redis.stream.config.RedisStreamRecoveryProperties;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pending 认领、成功确认与失败消息保留的模拟测试。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@SuppressWarnings("unchecked")
class RedisStreamPendingRecoveryTest {

    private static final String STREAM = "orders";
    private static final String GROUP = "workers";

    private RedisStreamRecoveryProperties recoveryProperties() {
        RedisStreamRecoveryProperties properties = new RedisStreamRecoveryProperties();
        properties.setMinIdle(Duration.ofMillis(10));
        properties.setMaxRetryDelay(Duration.ofSeconds(1));
        properties.setMaxAttempts(3);
        properties.setBatchSize(5);
        return properties;
    }

    @Test
    void successfulReclaimShouldProcessThenAcknowledge() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        StreamOperations<String, Object, Object> operations = mock(StreamOperations.class);
        when(redis.opsForStream()).thenReturn(operations);
        RecordId id = RecordId.of("100-0");
        PendingMessage p = new PendingMessage(id, Consumer.from(GROUP, "old-instance"), Duration.ofSeconds(5), 1);
        when(operations.pending(eq(STREAM), eq(GROUP), any(Range.class), eq(5L)))
                .thenReturn(new PendingMessages(GROUP, List.of(p)));

        MapRecord<String, Object, Object> raw = mock(MapRecord.class);
        when(raw.getId()).thenReturn(id);
        when(operations.claim(eq(STREAM), eq(GROUP), anyString(), eq(Duration.ofMillis(10)), eq(id)))
                .thenReturn(List.of(raw));
        ObjectRecord<String, String> mapped = mock(ObjectRecord.class);
        when(operations.map(raw, String.class)).thenReturn(mapped);
        when(operations.acknowledge(STREAM, GROUP, id)).thenReturn(1L);

        MessageConsumer consumer = mock(MessageConsumer.class);
        RedisStreamPendingRecovery recovery = new RedisStreamPendingRecovery(
                redis, streamProperties(), recoveryProperties(), consumer);
        assertThat(recovery.recoverOnce()).isEqualTo(1);

        var ordered = inOrder(consumer, operations);
        ordered.verify(consumer).accept(mapped);
        ordered.verify(operations).acknowledge(STREAM, GROUP, id);
    }

    @Test
    void failedReclaimShouldLeaveMessagePending() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        StreamOperations<String, Object, Object> operations = mock(StreamOperations.class);
        when(redis.opsForStream()).thenReturn(operations);
        RecordId id = RecordId.of("101-0");
        PendingMessage p = new PendingMessage(id, Consumer.from(GROUP, "old-instance"), Duration.ofSeconds(5), 1);
        when(operations.pending(eq(STREAM), eq(GROUP), any(Range.class), eq(5L)))
                .thenReturn(new PendingMessages(GROUP, List.of(p)));
        MapRecord<String, Object, Object> raw = mock(MapRecord.class);
        when(raw.getId()).thenReturn(id);
        when(operations.claim(eq(STREAM), eq(GROUP), anyString(), eq(Duration.ofMillis(10)), eq(id)))
                .thenReturn(List.of(raw));
        ObjectRecord<String, String> mapped = mock(ObjectRecord.class);
        when(operations.map(raw, String.class)).thenReturn(mapped);

        MessageConsumer consumer = mock(MessageConsumer.class);
        doThrow(new IllegalStateException("temporary failure")).when(consumer).accept(mapped);
        RedisStreamPendingRecovery recovery = new RedisStreamPendingRecovery(
                redis, streamProperties(), recoveryProperties(), consumer);
        assertThat(recovery.recoverOnce()).isZero();

        verify(consumer).accept(mapped);
        verify(operations, never()).acknowledge(STREAM, GROUP, id);
    }

    @Test
    void exhaustedAttemptsShouldPersistToDlqBeforeAcknowledge() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        StreamOperations<String, Object, Object> operations = mock(StreamOperations.class);
        when(redis.opsForStream()).thenReturn(operations);
        RecordId id = RecordId.of("103-0");
        PendingMessage p = new PendingMessage(id, Consumer.from(GROUP, "old"), Duration.ofMinutes(2), 3);
        when(operations.pending(eq(STREAM), eq(GROUP), any(Range.class), eq(5L)))
                .thenReturn(new PendingMessages(GROUP, List.of(p)));
        MapRecord<String, Object, Object> raw = mock(MapRecord.class);
        when(raw.getId()).thenReturn(id);
        when(raw.getValue()).thenReturn(java.util.Map.of("_raw", "test-payload"));
        when(operations.claim(eq(STREAM), eq(GROUP), anyString(), eq(Duration.ofMillis(40)), eq(id)))
                .thenReturn(List.of(raw));
        when(operations.add(any(org.springframework.data.redis.connection.stream.Record.class)))
                .thenReturn(RecordId.of("203-0"));
        when(operations.acknowledge(STREAM, GROUP, id)).thenReturn(1L);

        MessageConsumer consumer = mock(MessageConsumer.class);
        RedisStreamPendingRecovery recovery = new RedisStreamPendingRecovery(
                redis, streamProperties(), recoveryProperties(), consumer);
        assertThat(recovery.recoverOnce()).isEqualTo(1);
        var ordered = inOrder(operations);
        ordered.verify(operations).add(any(org.springframework.data.redis.connection.stream.Record.class));
        ordered.verify(operations).acknowledge(STREAM, GROUP, id);
        verifyNoInteractions(consumer);
    }

    @Test
    void failedDlqWriteMustNotAcknowledgeSource() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        StreamOperations<String, Object, Object> operations = mock(StreamOperations.class);
        when(redis.opsForStream()).thenReturn(operations);
        RecordId id = RecordId.of("104-0");
        PendingMessage p = new PendingMessage(id, Consumer.from(GROUP, "old"), Duration.ofMinutes(2), 3);
        when(operations.pending(eq(STREAM), eq(GROUP), any(Range.class), eq(5L)))
                .thenReturn(new PendingMessages(GROUP, List.of(p)));
        MapRecord<String, Object, Object> raw = mock(MapRecord.class);
        when(raw.getId()).thenReturn(id);
        when(raw.getValue()).thenReturn(java.util.Map.of("_raw", "test-payload"));
        when(operations.claim(eq(STREAM), eq(GROUP), anyString(), eq(Duration.ofMillis(40)), eq(id)))
                .thenReturn(List.of(raw));
        when(operations.add(any(org.springframework.data.redis.connection.stream.Record.class)))
                .thenThrow(new IllegalStateException("redis unavailable"));

        MessageConsumer consumer = mock(MessageConsumer.class);
        RedisStreamPendingRecovery recovery = new RedisStreamPendingRecovery(
                redis, streamProperties(), recoveryProperties(), consumer);
        assertThat(recovery.recoverOnce()).isZero();
        verify(operations, never()).acknowledge(STREAM, GROUP, id);
    }

    @Test
    void invalidRecoveryConfigurationShouldFailFast() {
        RedisStreamRecoveryProperties bad = recoveryProperties();
        bad.setBatchSize(0);
        assertThatThrownBy(bad::validate).isInstanceOf(IllegalArgumentException.class);
        bad.setBatchSize(32);
        bad.setMinIdle(Duration.ZERO);
        assertThatThrownBy(bad::validate).isInstanceOf(IllegalArgumentException.class);
    }

    private RedisStreamProperties streamProperties() {
        RedisStreamProperties properties = new RedisStreamProperties();
        properties.setStreamName(STREAM);
        properties.setConsumerGroup(GROUP);
        properties.setConsumerName("worker");
        return properties;
    }
}
