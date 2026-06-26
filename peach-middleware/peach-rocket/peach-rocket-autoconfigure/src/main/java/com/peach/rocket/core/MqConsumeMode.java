package com.peach.rocket.core;

/**
 * MQ 消费模式。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
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
