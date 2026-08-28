package com.peach.rocket.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.peach.rocket.core.MqMessageEnvelope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JacksonMQ消息Envelope记录Test。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */

class JacksonMqMessageEnvelopeRecordTest {

    private JacksonMqMessageCodec codec;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        codec = new JacksonMqMessageCodec(objectMapper);
    }

    @Test
    void shouldRoundTripEnvelopeWithRecordPayload() {
        SampleRecordPayload payload = new SampleRecordPayload("demo", 7);
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 27, 10, 30, 0);
        MqMessageEnvelope<SampleRecordPayload> original = MqMessageEnvelope.create(
                "msg-1",
                "orders",
                "created",
                "order-7",
                "peach-demo",
                SampleRecordPayload.class.getName(),
                1,
                Map.of("tenant", "t1"),
                payload,
                createdAt);

        byte[] encoded = codec.encode(original);
        MqMessageEnvelope<SampleRecordPayload> decoded = codec.decode(encoded, SampleRecordPayload.class);

        assertThat(decoded.messageId()).isEqualTo("msg-1");
        assertThat(decoded.topic()).isEqualTo("orders");
        assertThat(decoded.tag()).isEqualTo("created");
        assertThat(decoded.key()).isEqualTo("order-7");
        assertThat(decoded.producerApp()).isEqualTo("peach-demo");
        assertThat(decoded.payloadType()).isEqualTo(SampleRecordPayload.class.getName());
        assertThat(decoded.version()).isEqualTo(1);
        assertThat(decoded.headers()).containsEntry("tenant", "t1");
        assertThat(decoded.createdAt()).isEqualTo(createdAt);
        assertThat(decoded.payload()).isEqualTo(payload);
    }

    /**
     * Sample记录Payload值对象。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */

    private record SampleRecordPayload(String name, int count) {
    }
}
