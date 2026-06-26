package com.peach.rocket.core;

/**
 * RocketMQ 本地事务状态。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
public enum MqLocalTransactionState {

    /**
     * 提交事务消息。
     */
    COMMIT,

    /**
     * 回滚事务消息。
     */
    ROLLBACK,

    /**
     * 本地事务状态未知，等待 Broker 回查。
     */
    UNKNOWN
}
