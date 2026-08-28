package com.peach.rocket.core;

/**
 * MQLocal事务State枚举。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
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
