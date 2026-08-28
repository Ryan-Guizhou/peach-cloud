package com.peach.rocket.security;

/**
 * MQ载荷加密器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
public interface MqPayloadEncryptor {

    /**
     * 加密 payload 字节数组。
     *
     * @param plainBytes 明文字节数组
     * @param context 加密上下文
     * @return 加密结果
     */
    MqEncryptionResult encrypt(byte[] plainBytes, MqEncryptionContext context);

    /**
     * 解密 payload 字节数组。
     *
     * @param cipherBytes 密文字节数组
     * @param context 加密上下文
     * @return 明文字节数组
     */
    byte[] decrypt(byte[] cipherBytes, MqEncryptionContext context);
}
