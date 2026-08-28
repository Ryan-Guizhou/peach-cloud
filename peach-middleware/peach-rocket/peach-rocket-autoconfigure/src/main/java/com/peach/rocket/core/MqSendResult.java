package com.peach.rocket.core;

/**
 * MQ发送结果。
 * <p>该模型对 RocketMQ 原生发送结果做了最小抽象，只保留业务侧最常用的发送状态、消息 ID 和最终路由信息，
 * 便于日志记录、链路追踪和上层服务统一处理发送结果。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
public record MqSendResult(
        boolean success,
        String messageId,
        String topic,
        String tag,
        String key,
        String rawStatus) {

    /**
     * 创建发送结果构建器。
     *
     * @return 发送结果构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 是否发送成功。
     *
     * @return 发送成功时返回 {@code true}
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 构建器。
     */
    public static final class Builder {

        private boolean success;
        private String messageId;
        private String topic;
        private String tag;
        private String key;
        private String rawStatus;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder topic(String topic) {
            this.topic = topic;
            return this;
        }

        public Builder tag(String tag) {
            this.tag = tag;
            return this;
        }

        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public Builder rawStatus(String rawStatus) {
            this.rawStatus = rawStatus;
            return this;
        }

        public MqSendResult build() {
            return new MqSendResult(success, messageId, topic, tag, key, rawStatus);
        }
    }
}
