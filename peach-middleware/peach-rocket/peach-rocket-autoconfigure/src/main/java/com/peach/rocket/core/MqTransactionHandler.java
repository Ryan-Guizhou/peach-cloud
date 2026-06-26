package com.peach.rocket.core;

/**
 * RocketMQ 事务消息处理器。
 *
 * @param <T> payload 类型
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
public interface MqTransactionHandler<T> {

    /**
     * 执行本地事务。
     *
     * @param message 业务消息
     * @param transactionKey 事务键
     * @return 本地事务状态
     */
    MqLocalTransactionState executeLocalTransaction(T message, String transactionKey);

    /**
     * 回查本地事务状态。
     *
     * @param message 业务消息
     * @param transactionKey 事务键
     * @return 本地事务状态
     */
    MqLocalTransactionState checkLocalTransaction(T message, String transactionKey);
}
