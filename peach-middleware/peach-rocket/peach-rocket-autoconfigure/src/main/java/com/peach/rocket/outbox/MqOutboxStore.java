package com.peach.rocket.outbox;

import java.util.List;

/**
 * Outbox 可靠消息存储 SPI。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
public interface MqOutboxStore {

    /**
     * 保存待发送消息。
     *
     * @param event Outbox 事件
     */
    void save(MqOutboxEvent event);

    /**
     * 查询待发送消息。
     *
     * @param batchSize 单批数量
     * @return 待发送消息列表
     */
    List<MqOutboxEvent> findPending(int batchSize);

    /**
     * 标记消息发送成功。
     *
     * @param messageId 消息 ID
     */
    void markSent(String messageId);

    /**
     * 标记消息发送失败。
     *
     * @param messageId 消息 ID
     */
    void markFailed(String messageId);

    /**
     * 重置失败消息。
     *
     * @param messageId 消息 ID
     * @return true 表示重置成功
     */
    boolean replay(String messageId);
}
