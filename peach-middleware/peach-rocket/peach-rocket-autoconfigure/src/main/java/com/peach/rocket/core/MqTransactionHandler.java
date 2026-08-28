package com.peach.rocket.core;

/**
 * MQ事务处理器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 * @param <T> payload 类型
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
