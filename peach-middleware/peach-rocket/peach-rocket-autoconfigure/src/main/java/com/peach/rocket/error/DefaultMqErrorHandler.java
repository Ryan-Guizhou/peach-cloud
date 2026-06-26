package com.peach.rocket.error;

import com.peach.rocket.core.MqConsumeContext;
import com.peach.rocket.core.MqMessageEnvelope;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认 MQ 消费异常处理器。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
@Slf4j
public class DefaultMqErrorHandler implements MqErrorHandler {

    @Override
    public void handle(RuntimeException exception, MqMessageEnvelope<?> envelope, MqConsumeContext context) {
        log.error("[mq-consume-error] topic={} tag={} key={} messageId={} exception={}",
                context.getTopic(), context.getTag(), context.getKey(), context.getMessageId(), exception.getClass().getName(), exception);
    }
}
