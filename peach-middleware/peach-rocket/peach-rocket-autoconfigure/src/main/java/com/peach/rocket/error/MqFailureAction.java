package com.peach.rocket.error;

/**
 * MQFailureAction枚举。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
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
