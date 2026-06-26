package com.peach.rocket.security;

/**
 * MQ 加密上下文。
 *
 * <p>该上下文在加密和解密阶段向密钥提供者、加密策略和加密器传递统一元信息，便于根据 topic、payload
 * 类型、算法或密钥标识执行差异化处理。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
public class MqEncryptionContext {

    /**
     * 当前消息的真实 topic。
     */
    private final String topic;

    /**
     * 当前 payload 的 Java 类型名称。
     */
    private final String payloadType;

    /**
     * 当前加密或解密使用的算法标识。
     */
    private final String algorithm;

    /**
     * 当前使用的密钥标识。
     */
    private final String keyId;

    public MqEncryptionContext(String topic, String payloadType, String algorithm, String keyId) {
        this.topic = topic;
        this.payloadType = payloadType;
        this.algorithm = algorithm;
        this.keyId = keyId;
    }

    public String getTopic() {
        return topic;
    }

    public String getPayloadType() {
        return payloadType;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getKeyId() {
        return keyId;
    }
}
