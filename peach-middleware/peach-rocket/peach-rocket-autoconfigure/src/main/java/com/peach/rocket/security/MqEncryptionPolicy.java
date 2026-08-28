package com.peach.rocket.security;

/**
 * MQ加密策略。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
public interface MqEncryptionPolicy {

    /**
     * 判断指定消息是否需要加密。
     *
     * @param topic 真实 topic
     * @param payloadType payload 类型
     * @param payload payload 对象
     * @return true 表示需要加密
     */
    boolean shouldEncrypt(String topic, String payloadType, Object payload);
}
