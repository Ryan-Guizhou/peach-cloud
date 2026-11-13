package com.peach.rocket.codec;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peach.rocket.core.MqMessageEnvelope;
import com.peach.rocket.exception.MqException;
import com.peach.rocket.autoconfigure.PeachRocketProperties;
import java.io.IOException;
import java.lang.reflect.Type;

/**
 * JacksonMqMessageCodec相关类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
public class JacksonMqMessageCodec implements MqMessageCodec {

    private final ObjectMapper objectMapper;
    private final PeachRocketProperties properties;

    public JacksonMqMessageCodec(ObjectMapper objectMapper) {
        this(objectMapper, new PeachRocketProperties());
    }

    public JacksonMqMessageCodec(ObjectMapper objectMapper, PeachRocketProperties properties) {
        this.properties = properties;
        this.objectMapper = MqCodecSupport.mapper(objectMapper, properties);
    }

    @Override
    public byte[] encode(MqMessageEnvelope<?> envelope) {
        try {
            byte[] encoded = objectMapper.writeValueAsBytes(envelope);
            MqCodecSupport.envelope(envelope, encoded, properties);
            return encoded;
        } catch (IOException ex) {
            throw new MqException("Failed to encode MQ message envelope");
        }
    }

    @Override
    public <T> MqMessageEnvelope<T> decode(byte[] body, Class<T> payloadType) {
        return decode(body, (Type) payloadType);
    }

    @Override
    public <T> MqMessageEnvelope<T> decode(byte[] body, Type payloadType) {
        try {
            MqCodecSupport.body(body, properties);
            JavaType elementType = objectMapper.constructType(payloadType);
            JavaType javaType = objectMapper.getTypeFactory()
                    .constructParametricType(MqMessageEnvelope.class, elementType);
            MqMessageEnvelope<T> decoded = objectMapper.readValue(body, javaType);
            MqCodecSupport.envelope(decoded, body, properties);
            if (decoded.encrypted()) {
                throw new MqException("Encrypted MQ envelope requires the security codec");
            }
            return decoded;
        } catch (IOException ex) {
            throw new MqException("Failed to decode MQ message envelope");
        }
    }
}
