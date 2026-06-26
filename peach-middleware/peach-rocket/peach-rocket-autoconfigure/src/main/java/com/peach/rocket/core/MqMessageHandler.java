package com.peach.rocket.core;

/**
 * MQ 业务消息处理器。
 *
 * @param <T> payload 类型
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
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
