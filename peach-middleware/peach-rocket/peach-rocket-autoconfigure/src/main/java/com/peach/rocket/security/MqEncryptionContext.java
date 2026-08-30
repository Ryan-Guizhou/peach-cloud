package com.peach.rocket.security;

/**
 * MQ加密上下文。
 * <p>该上下文在加密和解密阶段向密钥提供者、加密策略和加密器传递统一元信息，便于根据 topic、payload
 * 类型、算法或密钥标识执行差异化处理。
 *
 * @param topic 消息主题
 * @param payloadType 业务载荷类型
 * @param algorithm 加密算法
 * @param keyId 密钥标识
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
public record MqEncryptionContext(String topic, String payloadType, String algorithm, String keyId) {
}
