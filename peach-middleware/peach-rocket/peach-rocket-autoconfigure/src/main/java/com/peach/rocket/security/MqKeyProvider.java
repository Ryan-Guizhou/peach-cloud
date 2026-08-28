package com.peach.rocket.security;

/**
 * MQ键提供者。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
public interface MqKeyProvider {

    /**
     * 根据密钥标识获取密钥字节数组。
     *
     * @param keyId 密钥标识
     * @return 密钥字节数组
     */
    byte[] getKey(String keyId);
}
