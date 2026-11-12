package com.peach.rocket.error;

import com.peach.rocket.core.MqConsumeContext;
import com.peach.rocket.core.MqMessageEnvelope;

/**
 * 消费者不可重试失败的持久化回执。
 *
 * <p>只有失败消息或补偿工单已经可靠持久化后才能返回 true。
 * 无可追踪失败记录时，框架将 SKIP 转为重试。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/12
 */
@FunctionalInterface
public interface MqFailureRecorder {

    MqFailureRecorder NOOP = (exception, envelope, context) -> false;

    /**
     * @param exception 业务异常
     * @param envelope 消息信封
     * @param context 消费上下文
     * @return 持久化失败回执成功并允许 ACK 时返回 true
     */
    boolean record(RuntimeException exception, MqMessageEnvelope<?> envelope, MqConsumeContext context);
}
