package com.peach.rocket.support;

import com.peach.rocket.autoconfigure.PeachRocketProperties;
import org.springframework.util.StringUtils;

/**
 * RocketMQMQ命名。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
public final class RocketMqNaming {

    private RocketMqNaming() {
    }

    /**
     * 规范化 topic 名称。
     *
     * @param topic 业务 topic
     * @param properties 配置属性
     * @return 真实 topic
     */
    public static String normalizeTopic(String topic, PeachRocketProperties properties) {
        if (!StringUtils.hasText(topic) || topic.contains("%")) {
            return topic;
        }
        if (!properties.getNaming().isAutoPrefixEnv()) {
            return topic;
        }
        String separator = properties.getNaming().getTopicSeparator();
        String prefix = properties.getNamespace() + separator + properties.getNaming().getTopicPrefix() + separator;
        return topic.startsWith(prefix) ? topic : prefix + topic;
    }

    /**
     * 规范化 consumer group。
     *
     * @param consumerGroup 消费者组
     * @return consumer group
     */
    public static String normalizeConsumerGroup(String consumerGroup) {
        return consumerGroup;
    }
}
