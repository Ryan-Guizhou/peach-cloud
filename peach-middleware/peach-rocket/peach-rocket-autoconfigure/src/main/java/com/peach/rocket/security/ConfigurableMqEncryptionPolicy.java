package com.peach.rocket.security;

import com.peach.rocket.annotation.MqEncrypted;
import com.peach.rocket.autoconfigure.PeachRocketProperties;
import org.springframework.util.CollectionUtils;

/**
 * 可配置的消息加密选择策略；全局开关、指定 Topic 或类型注解任一命中即可加密。
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
        if (!CollectionUtils.isEmpty(properties.getSecurity().getEncryptTopics())
                && properties.getSecurity().getEncryptTopics().stream().anyMatch(configured ->
                        com.peach.rocket.support.RocketMqNaming.normalizeTopic(configured, properties).equals(topic))) {
            return true;
        }
        return payload != null && payload.getClass().isAnnotationPresent(MqEncrypted.class);
    }
}
