package com.peach.rocket.core;

/**
 * MQConsumeMode枚举。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
public enum MqConsumeMode {

    /**
     * 并发消费。
     */
    CONCURRENTLY,

    /**
     * 顺序消费。
     */
    ORDERLY
}
