package com.peach.rocket.codec;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.core.MqMessageEnvelope;
import com.peach.rocket.exception.MqException;
import com.peach.rocket.producer.MqProducerMessageGuard;

/**
 * MQ 独立 JSON 解析器及信封预算；不修改应用共享 ObjectMapper。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13 15:00
 */
final class MqCodecSupport {
    private MqCodecSupport() { }

    static ObjectMapper mapper(ObjectMapper source, PeachRocketProperties properties) {
        ObjectMapper safe = source.copy().deactivateDefaultTyping();
        safe.getFactory().setStreamReadConstraints(StreamReadConstraints.builder()
                .maxNestingDepth(properties.getProtocol().getMaxDepth())
                .maxStringLength(properties.getProtocol().getMaxStringLength())
                .maxNumberLength(properties.getProtocol().getMaxNumberLength()).build());
        return safe;
    }

    static void body(byte[] body, PeachRocketProperties properties) {
        if (body == null || body.length == 0 || body.length > properties.getProducer().getMaxPayloadBytes()) {
            throw new MqException("Invalid MQ encoded payload size");
        }
    }

    static void envelope(MqMessageEnvelope<?> envelope, byte[] body, PeachRocketProperties properties) {
        if (envelope == null || envelope.version() < 1) {
            throw new MqException("Invalid MQ envelope version");
        }
        text(envelope.messageId(), 64, true);
        text(envelope.topic(), 128, true);
        text(envelope.tag(), 128, false);
        text(envelope.key(), 128, false);
        text(envelope.payloadType(), 256, true);
        text(envelope.producerApp(), 128, false);
        MqProducerMessageGuard.validate(body, envelope.headers(), envelope.key(), properties.getProducer());
    }

    private static void text(String value, int length, boolean required) {
        if ((required && (value == null || value.isBlank())) || (value != null
                && (value.length() > length || value.chars().anyMatch(Character::isISOControl)))) {
            throw new MqException("Invalid MQ envelope metadata");
        }
    }
}
