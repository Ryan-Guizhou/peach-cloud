package com.peach.rocket.security;

import com.peach.rocket.annotation.MqEncrypted;
import com.peach.rocket.autoconfigure.PeachRocketProperties;

/**
 * ConfigurableMQ加密策略。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
public class ConfigurableMqEncryptionPolicy implements MqEncryptionPolicy {

    private final PeachRocketProperties properties;

    public ConfigurableMqEncryptionPolicy(PeachRocketProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean shouldEncrypt(String topic, String payloadType, Object payload) {
        if (!properties.getSecurity().isEnabled()) {
            return false;
        }
        if (properties.getSecurity().isEncryptPayload()) {
            return true;
        }
        if (properties.getSecurity().getEncryptTopics().contains(topic)) {
            return true;
        }
        return payload != null && payload.getClass().isAnnotationPresent(MqEncrypted.class);
    }
}
