package com.peach.rocket.outbox;

/**
 * Outbox 失败消息重放服务。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
public interface MqOutboxReplayService {

    /**
     * 将失败消息重置为可重试状态。
     *
     * @param messageId Outbox 消息 ID
     * @return true 表示重置成功
     */
    boolean replay(String messageId);
}
