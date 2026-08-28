package com.peach.rocket.core;

/**
 * MQ消息处理器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 * @param <T> payload 类型
 */
public interface MqMessageHandler<T> {

    /**
     * 处理业务消息。
     *
     * @param message 业务消息
     * @param context 消费上下文
     */
    void handle(T message, MqConsumeContext context);
}
