package com.peach.rocket.codec;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peach.rocket.core.MqMessageEnvelope;
import com.peach.rocket.exception.MqException;
import java.io.IOException;

/**
 * 基于 Jackson 的 MQ 标准信封编解码器。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
public class JacksonMqMessageCodec implements MqMessageCodec {

    private final ObjectMapper objectMapper;

    public JacksonMqMessageCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] encode(MqMessageEnvelope<?> envelope) {
        try {
            return objectMapper.writeValueAsBytes(envelope);
        } catch (IOException ex) {
            throw new MqException("Failed to encode MQ message envelope", ex);
        }
    }

    @Override
    public <T> MqMessageEnvelope<T> decode(byte[] body, Class<T> payloadType) {
        try {
            JavaType javaType = objectMapper.getTypeFactory().constructParametricType(MqMessageEnvelope.class, payloadType);
            return objectMapper.readValue(body, javaType);
        } catch (IOException ex) {
            throw new MqException("Failed to decode MQ message envelope", ex);
        }
    }
}
