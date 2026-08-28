package com.peach.rocket.core;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * MQ消息Envelope值对象。
 * <p>所有发送到 RocketMQ 的业务消息都会先包装为统一信封，信封中包含路由信息、来源应用、消息版本、
 * 透传头、加密元信息以及实际业务 payload。消费端会基于同一模型完成解码、解密和业务分发。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 * @param <T> payload 类型
 */
public record MqMessageEnvelope<T>(
        String messageId,
        String topic,
        String tag,
        String key,
        String producerApp,
        String payloadType,
        int version,
        Map<String, String> headers,
        T payload,
        LocalDateTime createdAt,
        boolean encrypted,
        String encryptionAlgorithm,
        String encryptionKeyId) {

    public MqMessageEnvelope {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    public static <T> MqMessageEnvelope<T> create(
            String messageId,
            String topic,
            String tag,
            String key,
            String producerApp,
            String payloadType,
            int version,
            Map<String, String> headers,
            T payload,
            LocalDateTime createdAt) {
        return new MqMessageEnvelope<>(
                messageId, topic, tag, key, producerApp, payloadType, version, headers, payload, createdAt,
                false, null, null);
    }

    @SuppressWarnings("unchecked")
    public MqMessageEnvelope<T> withEncryptedPayload(Object cipherPayload, String algorithm, String keyId) {
        return new MqMessageEnvelope<>(
                messageId, topic, tag, key, producerApp, payloadType, version, headers,
                (T) cipherPayload, createdAt, true, algorithm, keyId);
    }

    @SuppressWarnings("unchecked")
    public <U> MqMessageEnvelope<U> withPayload(U newPayload) {
        return new MqMessageEnvelope<>(
                messageId, topic, tag, key, producerApp, payloadType, version, headers,
                newPayload, createdAt, encrypted, encryptionAlgorithm, encryptionKeyId);
    }
}
