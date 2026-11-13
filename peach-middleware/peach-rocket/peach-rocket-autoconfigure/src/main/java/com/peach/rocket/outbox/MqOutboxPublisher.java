package com.peach.rocket.outbox;

import com.peach.rocket.core.MqSendOptions;

/**
 * MQ发件箱Publisher接口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
public interface MqOutboxPublisher {

    /**
     * 写入待发送消息。
     *
     * @param payload 业务消息
     * @param options 发送参数
     * @param <T> 业务消息类型
     * @return Outbox 消息 ID
     */
    <T> String publish(T payload, MqSendOptions options);
    /** 写入独立事件；不保证与业务事务原子提交，旧实现需显式支持。 */
    default <T> String publishStandalone(T payload, MqSendOptions options) {
        throw new UnsupportedOperationException("Standalone Outbox publishing is not supported");
    }
}
