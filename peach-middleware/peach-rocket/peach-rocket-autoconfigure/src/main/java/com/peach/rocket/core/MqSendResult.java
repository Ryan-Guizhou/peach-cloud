package com.peach.rocket.core;

/**
 * MQ 发送结果。
 *
 * <p>该模型对 RocketMQ 原生发送结果做了最小抽象，只保留业务侧最常用的发送状态、消息 ID 和最终路由信息，
 * 便于日志记录、链路追踪和上层服务统一处理发送结果。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
public class MqSendResult {

    /**
     * 是否发送成功。
     */
    private boolean success;

    /**
     * RocketMQ 返回的消息 ID。
     */
    private String messageId;

    /**
     * 最终发送到 Broker 的 topic。
     */
    private String topic;

    /**
     * 最终发送使用的 tag。
     */
    private String tag;

    /**
     * 最终发送使用的业务 key。
     */
    private String key;

    /**
     * RocketMQ 原生发送状态字符串，例如 SEND_OK。
     */
    private String rawStatus;

    /**
     * 创建发送结果构建器。
     *
     * @return 发送结果构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
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

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getRawStatus() {
        return rawStatus;
    }

    public void setRawStatus(String rawStatus) {
        this.rawStatus = rawStatus;
    }

    public static class Builder {

        private final MqSendResult result = new MqSendResult();

        public Builder success(boolean success) {
            result.setSuccess(success);
            return this;
        }

        public Builder messageId(String messageId) {
            result.setMessageId(messageId);
            return this;
        }

        public Builder topic(String topic) {
            result.setTopic(topic);
            return this;
        }

        public Builder tag(String tag) {
            result.setTag(tag);
            return this;
        }

        public Builder key(String key) {
            result.setKey(key);
            return this;
        }

        public Builder rawStatus(String rawStatus) {
            result.setRawStatus(rawStatus);
            return this;
        }

        public MqSendResult build() {
            return result;
        }
    }
}
