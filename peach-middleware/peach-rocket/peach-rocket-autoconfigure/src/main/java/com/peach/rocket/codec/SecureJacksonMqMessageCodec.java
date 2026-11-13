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
import com.peach.rocket.security.MqEnvelopeAuthentication;
import com.peach.rocket.annotation.MqEncrypted;
import com.peach.rocket.support.RocketMqNaming;
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
        this.objectMapper = MqCodecSupport.mapper(objectMapper, properties);
        this.encryptor = encryptor;
        this.encryptionPolicy = encryptionPolicy;
        this.properties = properties;
    }

    @Override
    public byte[] encode(MqMessageEnvelope<?> envelope) {
        try {
            Object payload = envelope.payload();
            MqMessageEnvelope<?> toEncode = envelope;
            if (encryptionPolicy != null
                    && encryptionPolicy.shouldEncrypt(envelope.topic(), envelope.payloadType(), payload)) {
                if (encryptor == null) {
                    throw new MqException("Required MQ encryption component is unavailable");
                }
                byte[] plainBytes = objectMapper.writeValueAsBytes(payload);
                String algorithm = properties.getSecurity().getAlgorithm();
                String keyId = properties.getSecurity().getKeyId();
                MqEncryptionContext context = new MqEncryptionContext(
                        envelope.topic(), envelope.payloadType(), algorithm, keyId,
                        MqEnvelopeAuthentication.associatedData(envelope, algorithm, keyId));
                MqEncryptionResult result = encryptor.encrypt(plainBytes, context);
                toEncode = envelope.withEncryptedPayload(result.cipherBytes(), result.algorithm(), result.keyId());
            }
            byte[] encoded = objectMapper.writeValueAsBytes(toEncode);
            MqCodecSupport.envelope(toEncode, encoded, properties);
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
            JavaType rawType = objectMapper.getTypeFactory()
                    .constructParametricType(MqMessageEnvelope.class, Object.class);
            JavaType actualType = objectMapper.constructType(payloadType);
            MqMessageEnvelope<?> raw = objectMapper.readValue(body, rawType);
            MqCodecSupport.envelope(raw, body, properties);
            T actualPayload;
            if (raw.encrypted()) {
                if (encryptor == null) {
                    throw new MqException("Encrypted MQ payload requires a decryptor");
                }
                byte[] cipherBytes = objectMapper.convertValue(raw.payload(), byte[].class);
                boolean authenticated = "AES_GCM_AAD".equals(raw.encryptionAlgorithm());
                if (!authenticated && (!"AES_GCM".equals(raw.encryptionAlgorithm())
                        || !properties.getSecurity().isAcceptLegacyEncryption())) {
                    throw new MqException("Legacy or unsupported MQ encryption is not accepted");
                }
                MqEncryptionContext context = new MqEncryptionContext(
                        raw.topic(), raw.payloadType(), raw.encryptionAlgorithm(), raw.encryptionKeyId(),
                        authenticated ? MqEnvelopeAuthentication.associatedData(raw,
                                raw.encryptionAlgorithm(), raw.encryptionKeyId()) : null);
                actualPayload = objectMapper.readValue(encryptor.decrypt(cipherBytes, context), actualType);
            } else {
                boolean required = properties.getSecurity().isEncryptPayload()
                        || properties.getSecurity().getEncryptTopics().stream().anyMatch(topic ->
                                RocketMqNaming.normalizeTopic(topic, properties).equals(raw.topic()))
                        || actualType.getRawClass().isAnnotationPresent(MqEncrypted.class)
                        || (actualType.getContentType() != null
                                && actualType.getContentType().getRawClass().isAnnotationPresent(MqEncrypted.class));
                if (required || raw.encryptionAlgorithm() != null || raw.encryptionKeyId() != null) {
                    throw new MqException("Plaintext MQ envelope is not permitted by the receive policy");
                }
                actualPayload = objectMapper.convertValue(raw.payload(), actualType);
            }
            return raw.withPayload(actualPayload);
        } catch (IOException | IllegalArgumentException ex) {
            throw new MqException("Failed to decode MQ message envelope");
        }
    }
}
