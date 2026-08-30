package com.peach.rocket.security;

/**
 * MQ加密结果。
 * <p>该模型用于承载一次 payload 加密后的密文字节数组以及解密所需的算法和密钥标识，后续会写入
 * 标准消息信封，供消费端解密阶段读取。
 *
 * @param cipherBytes 密文字节数组
 * @param algorithm 加密算法
 * @param keyId 密钥标识
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
public record MqEncryptionResult(byte[] cipherBytes, String algorithm, String keyId) {
}
