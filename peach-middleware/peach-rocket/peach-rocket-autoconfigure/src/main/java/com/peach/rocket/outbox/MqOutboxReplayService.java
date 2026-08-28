package com.peach.rocket.outbox;

/**
 * MQ发件箱Replay服务类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
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
