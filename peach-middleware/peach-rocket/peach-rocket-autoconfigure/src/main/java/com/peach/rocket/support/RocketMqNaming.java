package com.peach.rocket.support;

import com.peach.rocket.autoconfigure.PeachRocketProperties;
import org.springframework.util.StringUtils;
import com.peach.rocket.exception.MqException;

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
        if (!StringUtils.hasText(topic) || !topic.matches("[A-Za-z0-9_.%-]{1,128}")) {
            throw new MqException("Invalid RocketMQ topic identifier");
        }
        if (topic.contains("%")) {
            if (properties.isProduction()) {
                throw new MqException("Use native Broker namespace configuration instead of embedded namespace");
            }
            return topic;
        }
        if (!properties.getNaming().isAutoPrefixEnv()) {
            return topic;
        }
        String separator = properties.getNaming().getTopicSeparator();
        String prefix = properties.getNamespace() + separator + properties.getNaming().getTopicPrefix() + separator;
        String normalized = topic.startsWith(prefix) ? topic : prefix + topic;
        if (normalized.length() > 128) {
            throw new MqException("RocketMQ topic identifier is too long");
        }
        return normalized;
    }

    /**
     * 规范化 consumer group。
     *
     * @param consumerGroup 消费者组
     * @return consumer group
     */
    public static String normalizeConsumerGroup(String consumerGroup) {
        if (!StringUtils.hasText(consumerGroup) || !consumerGroup.matches("[A-Za-z0-9_.%-]{1,128}")) {
            throw new MqException("consumerGroup must not be blank");
        }
        return consumerGroup;
    }

    /**
     * 解析当前应用内生效的 Consumer Group 名称。默认保持旧名称以保留消费位点，
     * 只有显式开启配置时才添加环境/应用前缀。
     *
     * @param consumerGroup 声明的业务组名
     * @param properties RocketMQ 配置
     * @return 最终注册且用于幂等键的组名
     */
    public static String normalizeConsumerGroup(String consumerGroup, PeachRocketProperties properties) {
        String group = normalizeConsumerGroup(consumerGroup);
        if (properties.isProduction() && group.contains("%")) {
            throw new MqException("Use native Broker namespace configuration for consumer groups");
        }
        if (!properties.getNaming().isAutoPrefixConsumerGroup() || group.contains("%")) {
            return group;
        }
        String separator = properties.getNaming().getTopicSeparator();
        String prefix = properties.getNamespace() + separator + properties.getNaming().getGroupPrefix() + separator;
        String normalized = group.startsWith(prefix) ? group : prefix + group;
        if (normalized.length() > 128) {
            throw new MqException("RocketMQ consumer group is too long");
        }
        return normalized;
    }
}
