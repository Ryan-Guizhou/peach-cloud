package com.peach.rocket.outbox;

import com.peach.rocket.core.MqSendOptions;

/**
 * Outbox 可靠消息发布入口。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
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
}
