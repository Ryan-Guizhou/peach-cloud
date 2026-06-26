package com.peach.rocket.core;

import java.util.concurrent.CompletableFuture;

/**
 * MQ 统一发送入口。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
public interface MqPublisher {

    /**
     * 使用事件注解或默认配置发送同步消息。
     *
     * @param payload 业务消息
     * @param <T> 业务消息类型
     * @return 发送结果
     */
    <T> MqSendResult publish(T payload);

    /**
     * 使用显式发送参数发送同步消息。
     *
     * @param payload 业务消息
     * @param options 发送参数
     * @param <T> 业务消息类型
     * @return 发送结果
     */
    <T> MqSendResult publish(T payload, MqSendOptions options);

    /**
     * 异步发送消息。
     *
     * @param payload 业务消息
     * @param <T> 业务消息类型
     * @return 异步发送结果
     */
    <T> CompletableFuture<MqSendResult> publishAsync(T payload);

    /**
     * 使用显式发送参数异步发送消息。
     *
     * @param payload 业务消息
     * @param options 发送参数
     * @param <T> 业务消息类型
     * @return 异步发送结果
     */
    <T> CompletableFuture<MqSendResult> publishAsync(T payload, MqSendOptions options);

    /**
     * 单向发送消息，不等待 broker 确认。
     *
     * @param payload 业务消息
     * @param options 发送参数
     * @param <T> 业务消息类型
     */
    <T> void publishOneWay(T payload, MqSendOptions options);

    /**
     * 顺序发送消息。
     *
     * @param payload 业务消息
     * @param shardingKey 分片键
     * @param <T> 业务消息类型
     * @return 发送结果
     */
    <T> MqSendResult publishOrderly(T payload, String shardingKey);

    /**
     * 发送延迟消息。
     *
     * @param payload 业务消息
     * @param delay 延迟参数
     * @param <T> 业务消息类型
     * @return 发送结果
     */
    <T> MqSendResult publishDelay(T payload, MqDelay delay);

    /**
     * 发送 RocketMQ 事务消息。
     *
     * @param payload 业务消息
     * @param transactionKey 事务键
     * @param <T> 业务消息类型
     * @return 发送结果
     */
    <T> MqSendResult publishTransaction(T payload, String transactionKey);

    /**
     * 使用显式发送参数发送 RocketMQ 事务消息。
     *
     * @param payload 业务消息
     * @param transactionKey 事务键
     * @param options 发送参数
     * @param <T> 业务消息类型
     * @return 发送结果
     */
    <T> MqSendResult publishTransaction(T payload, String transactionKey, MqSendOptions options);
}
