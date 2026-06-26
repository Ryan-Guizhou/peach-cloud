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
 * 支持 payload 加密的 Jackson 消息编解码器。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
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
    @SuppressWarnings({"rawtypes", "unchecked"})
    public byte[] encode(MqMessageEnvelope<?> envelope) {
        try {
            Object payload = envelope.getPayload();
            if (encryptionPolicy != null && encryptor != null
                    && encryptionPolicy.shouldEncrypt(envelope.getTopic(), envelope.getPayloadType(), payload)) {
                byte[] plainBytes = objectMapper.writeValueAsBytes(payload);
                MqEncryptionContext context = new MqEncryptionContext(envelope.getTopic(), envelope.getPayloadType(), properties.getSecurity().getAlgorithm(), properties.getSecurity().getKeyId());
                MqEncryptionResult result = encryptor.encrypt(plainBytes, context);
                ((MqMessageEnvelope) envelope).setPayload(result.getCipherBytes());
                envelope.setEncrypted(true);
                envelope.setEncryptionAlgorithm(result.getAlgorithm());
                envelope.setEncryptionKeyId(result.getKeyId());
            }
            return objectMapper.writeValueAsBytes(envelope);
        } catch (IOException ex) {
            throw new MqException("Failed to encode MQ message envelope", ex);
        }
    }

    @Override
    public <T> MqMessageEnvelope<T> decode(byte[] body, Class<T> payloadType) {
        try {
            JavaType javaType = objectMapper.getTypeFactory().constructParametricType(MqMessageEnvelope.class, Object.class);
            MqMessageEnvelope<?> raw = objectMapper.readValue(body, javaType);
            Object payload = raw.getPayload();
            T actualPayload;
            if (raw.isEncrypted()) {
                byte[] cipherBytes = objectMapper.convertValue(payload, byte[].class);
                MqEncryptionContext context = new MqEncryptionContext(raw.getTopic(), raw.getPayloadType(), raw.getEncryptionAlgorithm(), raw.getEncryptionKeyId());
                actualPayload = objectMapper.readValue(encryptor.decrypt(cipherBytes, context), payloadType);
            } else {
                actualPayload = objectMapper.convertValue(payload, payloadType);
            }
            MqMessageEnvelope<T> envelope = new MqMessageEnvelope<T>();
            envelope.setMessageId(raw.getMessageId());
            envelope.setTopic(raw.getTopic());
            envelope.setTag(raw.getTag());
            envelope.setKey(raw.getKey());
            envelope.setProducerApp(raw.getProducerApp());
            envelope.setPayloadType(raw.getPayloadType());
            envelope.setVersion(raw.getVersion());
            envelope.setHeaders(raw.getHeaders());
            envelope.setCreatedAt(raw.getCreatedAt());
            envelope.setEncrypted(raw.isEncrypted());
            envelope.setEncryptionAlgorithm(raw.getEncryptionAlgorithm());
            envelope.setEncryptionKeyId(raw.getEncryptionKeyId());
            envelope.setPayload(actualPayload);
            return envelope;
        } catch (IOException ex) {
            throw new MqException("Failed to decode MQ message envelope", ex);
        }
    }
}
