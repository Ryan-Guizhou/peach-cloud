package com.peach.rocket.codec;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.core.MqMessageEnvelope;
import com.peach.rocket.security.AesGcmMqPayloadEncryptor;
import com.peach.rocket.security.ConfigMqKeyProvider;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 加密泛型消息及历史密钥轮换后的解码验证。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13
 */
class SecureJacksonMqGenericPayloadTest {

    @Test
    void shouldDecryptHistoricalKeyIntoGenericPayloadAfterRotation() {
        PeachRocketProperties properties = new PeachRocketProperties();
        properties.getSecurity().setKeyId("old");
        properties.getSecurity().setKeys(Map.of(
                "old", "0123456789abcdef",
                "new", "abcdef0123456789"));
        SecureJacksonMqMessageCodec codec = new SecureJacksonMqMessageCodec(
                new ObjectMapper().findAndRegisterModules(),
                new AesGcmMqPayloadEncryptor(new ConfigMqKeyProvider(properties)),
                (topic, type, payload) -> true, properties);
        MqMessageEnvelope<List<Payload>> envelope = MqMessageEnvelope.create(
                "secure-1", "orders", "created", "1", "app", List.class.getName(), 1,
                Map.of(), List.of(new Payload("ok")), LocalDateTime.now());

        byte[] encrypted = codec.encode(envelope);
        properties.getSecurity().setKeyId("new");

        MqMessageEnvelope<List<Payload>> decoded = codec.decode(encrypted,
                new TypeReference<List<Payload>>() { }.getType());
        assertEquals("ok", decoded.payload().get(0).name());
    }

    private record Payload(String name) {
    }
}
