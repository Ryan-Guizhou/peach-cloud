package com.peach.rocket.codec;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.core.MqMessageEnvelope;
import com.peach.rocket.exception.MqException;
import com.peach.rocket.security.MqEncryptionContext;
import com.peach.rocket.security.MqEncryptionPolicy;
import com.peach.rocket.security.MqEncryptionResult;
import com.peach.rocket.security.MqPayloadEncryptor;
import java.io.IOException;

/**
 * SecureJacksonMqMessageCodec相关类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
public class SecureJacksonMqMessageCodec implements MqMessageCodec {

    private final ObjectMapper objectMapper;
    private final MqPayloadEncryptor encryptor;
    private final MqEncryptionPolicy encryptionPolicy;
    private final PeachRocketProperties properties;

    public SecureJacksonMqMessageCodec(ObjectMapper objectMapper, MqPayloadEncryptor encryptor, MqEncryptionPolicy encryptionPolicy, PeachRocketProperties properties) {
        this.objectMapper = objectMapper;
        this.encryptor = encryptor;
        this.encryptionPolicy = encryptionPolicy;
        this.properties = properties;
    }

    @Override
    public byte[] encode(MqMessageEnvelope<?> envelope) {
        try {
            Object payload = envelope.payload();
            MqMessageEnvelope<?> toEncode = envelope;
            if (encryptionPolicy != null && encryptor != null
                    && encryptionPolicy.shouldEncrypt(envelope.topic(), envelope.payloadType(), payload)) {
                byte[] plainBytes = objectMapper.writeValueAsBytes(payload);
                MqEncryptionContext context = new MqEncryptionContext(
                        envelope.topic(), envelope.payloadType(),
                        properties.getSecurity().getAlgorithm(), properties.getSecurity().getKeyId());
                MqEncryptionResult result = encryptor.encrypt(plainBytes, context);
                toEncode = envelope.withEncryptedPayload(result.cipherBytes(), result.algorithm(), result.keyId());
            }
            return objectMapper.writeValueAsBytes(toEncode);
        } catch (IOException ex) {
            throw new MqException("Failed to encode MQ message envelope", ex);
        }
    }

    @Override
    public <T> MqMessageEnvelope<T> decode(byte[] body, Class<T> payloadType) {
        try {
            JavaType javaType = objectMapper.getTypeFactory().constructParametricType(MqMessageEnvelope.class, Object.class);
            MqMessageEnvelope<?> raw = objectMapper.readValue(body, javaType);
            Object payload = raw.payload();
            T actualPayload;
            if (raw.encrypted()) {
                byte[] cipherBytes = objectMapper.convertValue(payload, byte[].class);
                MqEncryptionContext context = new MqEncryptionContext(
                        raw.topic(), raw.payloadType(), raw.encryptionAlgorithm(), raw.encryptionKeyId());
                actualPayload = objectMapper.readValue(encryptor.decrypt(cipherBytes, context), payloadType);
            } else {
                actualPayload = objectMapper.convertValue(payload, payloadType);
            }
            return raw.withPayload(actualPayload);
        } catch (IOException ex) {
            throw new MqException("Failed to decode MQ message envelope", ex);
        }
    }
}
