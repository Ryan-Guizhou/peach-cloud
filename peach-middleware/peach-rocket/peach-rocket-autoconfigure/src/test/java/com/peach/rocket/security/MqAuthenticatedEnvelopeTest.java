package com.peach.rocket.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.codec.SecureJacksonMqMessageCodec;
import com.peach.rocket.core.MqMessageEnvelope;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 认证信封及明文降级防护回归。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13 15:00
 */
class MqAuthenticatedEnvelopeTest {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final PeachRocketProperties properties = new PeachRocketProperties();

    private SecureJacksonMqMessageCodec codec() {
        properties.getSecurity().setEnabled(true);
        properties.getSecurity().setEncryptPayload(true);
        return new SecureJacksonMqMessageCodec(mapper,
                new AesGcmMqPayloadEncryptor(id -> new byte[32]),
                new ConfigurableMqEncryptionPolicy(properties), properties);
    }

    private MqMessageEnvelope<Map<String, Integer>> envelope() {
        return MqMessageEnvelope.create("event-1", "orders", "created", "order-1", "app",
                "map", 1, Map.of("tenant", "tenant-1"), Map.of("amount", 10), null);
    }

    @Test
    void rejectPlaintextWhenEncryptionRequired() throws Exception {
        var codec = codec();
        assertThrows(RuntimeException.class, () -> codec.decode(mapper.writeValueAsBytes(envelope()), Map.class));
    }

    @Test
    void rejectAlteredRoutingAndIdentity() throws Exception {
        var codec = codec();
        byte[] encoded = codec.encode(envelope());
        assertEquals(10, codec.decode(encoded, Map.class).payload().get("amount"));
        for (String field : new String[]{"topic", "tag", "key", "messageId", "producerApp", "payloadType"}) {
            ObjectNode altered = (ObjectNode) mapper.readTree(encoded);
            altered.put(field, "altered");
            assertThrows(RuntimeException.class, () -> codec.decode(mapper.writeValueAsBytes(altered), Map.class), field);
        }
    }

    @Test
    void rejectAlteredHeaderAndAlgorithm() throws Exception {
        var codec = codec();
        ObjectNode altered = (ObjectNode) mapper.readTree(codec.encode(envelope()));
        ((ObjectNode) altered.get("headers")).put("tenant", "tenant-2");
        assertThrows(RuntimeException.class, () -> codec.decode(mapper.writeValueAsBytes(altered), Map.class));
        ObjectNode algorithmAltered = (ObjectNode) mapper.readTree(codec.encode(envelope()));
        algorithmAltered.put("encryptionAlgorithm", "unknown");
        byte[] body = mapper.writeValueAsBytes(algorithmAltered);
        assertThrows(RuntimeException.class, () -> codec.decode(body, Map.class));
    }
}
