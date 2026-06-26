package com.peach.rocket.error;

/**
 * MQ 消费失败处理动作。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
public enum MqFailureAction {

    /**
     * 交由 RocketMQ 重试。
     */
    RETRY,

    /**
     * 跳过重试并确认消费成功。
     */
    SKIP
}
