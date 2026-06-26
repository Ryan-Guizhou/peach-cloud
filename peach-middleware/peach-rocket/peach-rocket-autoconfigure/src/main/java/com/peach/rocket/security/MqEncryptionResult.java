package com.peach.rocket.security;

/**
 * MQ 加密结果。
 *
 * <p>该模型用于承载一次 payload 加密后的密文字节数组以及解密所需的算法和密钥标识，后续会写入
 * 标准消息信封，供消费端解密阶段读取。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
public class MqEncryptionResult {

    /**
     * 加密后的密文字节数组。
     */
    private final byte[] cipherBytes;

    /**
     * 当前密文对应的算法标识。
     */
    private final String algorithm;

    /**
     * 当前密文对应的密钥标识。
     */
    private final String keyId;

    public MqEncryptionResult(byte[] cipherBytes, String algorithm, String keyId) {
        this.cipherBytes = cipherBytes;
        this.algorithm = algorithm;
        this.keyId = keyId;
    }

    public byte[] getCipherBytes() {
        return cipherBytes;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getKeyId() {
        return keyId;
    }
}
