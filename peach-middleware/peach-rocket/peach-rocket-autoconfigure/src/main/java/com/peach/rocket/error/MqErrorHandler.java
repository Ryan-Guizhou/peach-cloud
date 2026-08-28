package com.peach.rocket.error;

import com.peach.rocket.core.MqConsumeContext;
import com.peach.rocket.core.MqMessageEnvelope;

/**
 * MQError处理器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
public interface MqErrorHandler {

    /**
     * 处理消费异常。
     *
     * @param exception 消费异常
     * @param envelope 消息信封
     * @param context 消费上下文
     */
    void handle(RuntimeException exception, MqMessageEnvelope<?> envelope, MqConsumeContext context);
}
