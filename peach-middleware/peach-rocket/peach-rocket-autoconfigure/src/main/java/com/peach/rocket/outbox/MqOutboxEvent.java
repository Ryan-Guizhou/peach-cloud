package com.peach.rocket.outbox;

import com.peach.rocket.core.MqSendOptions;
import java.time.LocalDateTime;

/**
 * Outbox 可靠消息事件。
 *
 * <p>该模型表示一条已经持久化到 Outbox 存储中的待投递消息，既包含消息体与目标路由，也包含投递状态、
 * 重试次数和创建更新时间，供调度器扫描、发送和重放使用。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
public class MqOutboxEvent {

    /**
     * 业务侧生成的消息唯一标识。
     */
    private String messageId;

    /**
     * 已序列化后的消息体。
     */
    private byte[] body;

    /**
     * 目标 topic。
     */
    private String topic;

    /**
     * 目标 tag，可以为空。
     */
    private String tag;

    /**
     * 业务主键或业务幂等键。
     */
    private String businessKey;

    /**
     * 发送附加参数，例如延迟级别、顺序分片键和 headers。
     */
    private MqSendOptions options;

    /**
     * 当前 Outbox 状态。
     */
    private MqOutboxStatus status;

    /**
     * 已重试次数。
     */
    private int retryCount;

    /**
     * 写入 Outbox 的时间。
     */
    private LocalDateTime createdAt;

    /**
     * 最近一次状态变更时间。
     */
    private LocalDateTime updatedAt;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    public MqSendOptions getOptions() {
        return options;
    }

    public void setOptions(MqSendOptions options) {
        this.options = options;
    }

    public MqOutboxStatus getStatus() {
        return status;
    }

    public void setStatus(MqOutboxStatus status) {
        this.status = status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}