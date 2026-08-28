package com.peach.rocket.error;

import com.peach.rocket.core.MqConsumeContext;
import com.peach.rocket.core.MqMessageEnvelope;

/**
 * DefaultMqExceptionClassifier相关类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
public class DefaultMqExceptionClassifier implements MqExceptionClassifier {

    @Override
    public MqFailureAction classify(RuntimeException exception, MqMessageEnvelope<?> envelope, MqConsumeContext context) {
        return MqFailureAction.RETRY;
    }
}
