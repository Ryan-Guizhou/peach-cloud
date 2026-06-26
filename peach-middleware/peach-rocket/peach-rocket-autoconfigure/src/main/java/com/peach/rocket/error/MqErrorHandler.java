package com.peach.rocket.error;

import com.peach.rocket.core.MqConsumeContext;
import com.peach.rocket.core.MqMessageEnvelope;

/**
 * MQ 消费异常处理器。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
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
