package com.peach.rocket.outbox;

import com.peach.rocket.core.MqSendOptions;
import java.time.LocalDateTime;

/**
 * MQ发件箱事件。
 * <p>该模型表示一条已经持久化到 Outbox 存储中的待投递消息，既包含消息体与目标路由，也包含投递状态、
 * 重试次数和创建更新时间，供调度器扫描、发送和重放使用。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
public record MqOutboxEvent(
        String messageId,
        byte[] body,
        String topic,
        String tag,
        String businessKey,
        MqSendOptions options,
        MqOutboxStatus status,
        int retryCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public MqOutboxEvent withStatus(MqOutboxStatus newStatus) {
        return new MqOutboxEvent(
                messageId, body, topic, tag, businessKey, options, newStatus, retryCount, createdAt, updatedAt);
    }

    public MqOutboxEvent withRetryCount(int newRetryCount) {
        return new MqOutboxEvent(
                messageId, body, topic, tag, businessKey, options, status, newRetryCount, createdAt, updatedAt);
    }
}
