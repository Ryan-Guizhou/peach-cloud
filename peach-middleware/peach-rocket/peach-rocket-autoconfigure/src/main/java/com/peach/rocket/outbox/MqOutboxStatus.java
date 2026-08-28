package com.peach.rocket.outbox;

/**
 * MQ发件箱Status枚举。
 * <p>该状态用于描述一条 Outbox 消息从写入本地存储到成功投递 RocketMQ 过程中的生命周期阶段。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
public enum MqOutboxStatus {

    /**
     * 已写入 Outbox，但尚未开始投递。
     */
    INIT,

    /**
     * 已被调度器选中并开始发送。
     */
    SENDING,

    /**
     * 已成功投递到 RocketMQ。
     */
    SENT,

    /**
     * 本次投递失败，等待下一轮重试。
     */
    RETRY,

    /**
     * 已达到失败终态，需要人工介入或显式重放。
     */
    FAILED
}
