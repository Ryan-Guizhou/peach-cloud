package com.peach.rocket.error;

import com.peach.rocket.core.MqConsumeContext;
import com.peach.rocket.core.MqMessageEnvelope;

/**
 * 默认 MQ 消费异常分类器。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
public class DefaultMqExceptionClassifier implements MqExceptionClassifier {

    @Override
    public MqFailureAction classify(RuntimeException exception, MqMessageEnvelope<?> envelope, MqConsumeContext context) {
        return MqFailureAction.RETRY;
    }
}
