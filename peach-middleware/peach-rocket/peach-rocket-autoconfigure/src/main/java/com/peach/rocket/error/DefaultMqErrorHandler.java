package com.peach.rocket.error;

import com.peach.rocket.core.MqConsumeContext;
import com.peach.rocket.core.MqMessageEnvelope;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认MQError处理器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
@Slf4j
public class DefaultMqErrorHandler implements MqErrorHandler {

    @Override
    public void handle(RuntimeException exception, MqMessageEnvelope<?> envelope, MqConsumeContext context) {
        log.error("[mq-consume-error] topic={} tag={} key={} messageId={} exception={}",
                context.topic(), context.tag(), context.key(), context.messageId(), exception.getClass().getName(), exception);
    }
}
