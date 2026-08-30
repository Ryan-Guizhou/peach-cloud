package com.peach.rocket.core;

import java.util.Map;

/**
 * MQConsume上下文。
 * <p>业务消费者在处理消息时，可通过该上下文读取消息元信息和透传头信息，而不需要直接依赖 RocketMQ
 * 原生消息对象，从而保持业务代码和消息中间件实现解耦。
 *
 * @param messageId 消息ID
 * @param topic 消息主题
 * @param tag 消息标签
 * @param key 业务消息键
 * @param reconsumeTimes RocketMQ 重试消费次数
 * @param headers 透传消息头
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
public record MqConsumeContext(
        String messageId,
        String topic,
        String tag,
        String key,
        int reconsumeTimes,
        Map<String, String> headers) {

    public MqConsumeContext {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }
}
