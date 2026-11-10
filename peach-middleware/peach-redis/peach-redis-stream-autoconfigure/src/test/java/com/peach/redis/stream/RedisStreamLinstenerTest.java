package com.peach.redis.stream;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.Mockito.*;

/**
 * 验证消费组成功后才 ACK，失败消息留在 PEL 的调用语义。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
class RedisStreamLinstenerTest {

    @Test
    @SuppressWarnings("unchecked")
    void successfulConsumerShouldAcknowledgeRecord() {
        MessageConsumer consumer = mock(MessageConsumer.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        StreamOperations<String, Object, Object> operations = mock(StreamOperations.class);
        when(redis.opsForStream()).thenReturn(operations);
        ObjectRecord<String, String> record = StreamRecords.newRecord()
                .in("order-status").ofObject("created").withId(RecordId.of("123-0"));
        when(operations.acknowledge("order-status", "orders", record.getId())).thenReturn(1L);

        new RedisStreamLinstener(consumer, redis, "orders").onMessage(record);

        verify(consumer).accept(record);
        verify(operations).acknowledge("order-status", "orders", record.getId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void failedConsumerMustNotAcknowledgeRecord() {
        MessageConsumer consumer = mock(MessageConsumer.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        StreamOperations<String, Object, Object> operations = mock(StreamOperations.class);
        when(redis.opsForStream()).thenReturn(operations);
        ObjectRecord<String, String> record = StreamRecords.newRecord()
                .in("order-status").ofObject("created").withId(RecordId.of("124-0"));
        doThrow(new IllegalStateException("business operation failed")).when(consumer).accept(record);

        new RedisStreamLinstener(consumer, redis, "orders").onMessage(record);

        verify(consumer).accept(record);
        verifyNoInteractions(operations);
    }
}
