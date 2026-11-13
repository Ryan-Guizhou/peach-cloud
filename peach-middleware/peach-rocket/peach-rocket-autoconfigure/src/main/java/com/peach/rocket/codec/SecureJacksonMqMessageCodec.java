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
import java.lang.reflect.Type;

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
        return decode(body, (Type) payloadType);
    }

    @Override
    public <T> MqMessageEnvelope<T> decode(byte[] body, Type payloadType) {
        try {
            JavaType rawType = objectMapper.getTypeFactory()
                    .constructParametricType(MqMessageEnvelope.class, Object.class);
            JavaType actualType = objectMapper.constructType(payloadType);
            MqMessageEnvelope<?> raw = objectMapper.readValue(body, rawType);
            T actualPayload;
            if (raw.encrypted()) {
                if (encryptor == null) {
                    throw new MqException("Encrypted MQ payload requires a decryptor");
                }
                byte[] cipherBytes = objectMapper.convertValue(raw.payload(), byte[].class);
                MqEncryptionContext context = new MqEncryptionContext(
                        raw.topic(), raw.payloadType(), raw.encryptionAlgorithm(), raw.encryptionKeyId());
                actualPayload = objectMapper.readValue(encryptor.decrypt(cipherBytes, context), actualType);
            } else {
                actualPayload = objectMapper.convertValue(raw.payload(), actualType);
            }
            return raw.withPayload(actualPayload);
        } catch (IOException ex) {
            throw new MqException("Failed to decode MQ message envelope", ex);
        }
    }
}
