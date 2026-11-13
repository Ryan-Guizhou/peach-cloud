package com.peach.rocket.codec;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peach.rocket.core.MqMessageEnvelope;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * 泛型 Payload 编解码回归。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13
 */
class JacksonMqParameterizedPayloadTest {

    @Test
    void shouldPreserveNestedGenericPayloadTypes() {
        JacksonMqMessageCodec codec = new JacksonMqMessageCodec(new ObjectMapper().findAndRegisterModules());
        MqMessageEnvelope<List<Payload>> envelope = MqMessageEnvelope.create(
                "event-1", "orders", "created", "1", "app", List.class.getName(), 1,
                Map.of(), List.of(new Payload("one")), LocalDateTime.now());

        MqMessageEnvelope<List<Payload>> restored = codec.decode(codec.encode(envelope),
                new TypeReference<List<Payload>>() { }.getType());

        assertInstanceOf(Payload.class, restored.payload().get(0));
        assertEquals("one", restored.payload().get(0).name());
    }

    private record Payload(String name) {
    }
}
