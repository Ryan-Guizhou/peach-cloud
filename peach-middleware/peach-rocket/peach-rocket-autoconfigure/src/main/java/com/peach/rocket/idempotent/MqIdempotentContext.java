package com.peach.rocket.idempotent;

import java.time.Duration;

/**
 * MQ 幂等上下文。
 *
 * <p>该模型用于把一次消费在幂等判断阶段所需的关键信息收敛到统一对象中，便于不同幂等存储实现共享同一套
 * 输入语义，例如内存实现、JDBC 实现、Redis 实现或业务自定义实现。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
public class MqIdempotentContext {

    /**
     * 幂等键。
     */
    private final String idempotentKey;

    /**
     * 当前消费者组。
     */
    private final String consumerGroup;

    /**
     * 当前消息 topic。
     */
    private final String topic;

    /**
     * 当前消息 tag，可以为空。
     */
    private final String tag;

    /**
     * 业务 key，可以为空。
     */
    private final String businessKey;

    /**
     * 消息唯一标识。
     */
    private final String messageId;

    /**
     * 幂等记录有效期。
     */
    private final Duration expire;

    /**
     * 创建幂等上下文。
     *
     * @param idempotentKey 幂等键
     * @param consumerGroup 消费者组
     * @param topic 消息 topic
     * @param tag 消息 tag
     * @param businessKey 业务 key
     * @param messageId 消息唯一标识
     * @param expire 幂等有效期
     */
    public MqIdempotentContext(String idempotentKey, String consumerGroup, String topic, String tag, String businessKey, String messageId, Duration expire) {
        this.idempotentKey = idempotentKey;
        this.consumerGroup = consumerGroup;
        this.topic = topic;
        this.tag = tag;
        this.businessKey = businessKey;
        this.messageId = messageId;
        this.expire = expire;
    }

    public String getIdempotentKey() {
        return idempotentKey;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public String getTopic() {
        return topic;
    }

    public String getTag() {
        return tag;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public String getMessageId() {
        return messageId;
    }

    public Duration getExpire() {
        return expire;
    }
}
