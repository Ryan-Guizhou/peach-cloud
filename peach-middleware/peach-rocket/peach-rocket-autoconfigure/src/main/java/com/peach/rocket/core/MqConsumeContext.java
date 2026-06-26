package com.peach.rocket.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MQ 消费上下文。
 *
 * <p>业务消费者在处理消息时，可通过该上下文读取消息元信息和透传头信息，而不需要直接依赖 RocketMQ
 * 原生消息对象，从而保持业务代码和消息中间件实现解耦。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
public class MqConsumeContext {

    /**
     * 当前消费消息的唯一标识。
     */
    private final String messageId;

    /**
     * 当前消费消息的 topic。
     */
    private final String topic;

    /**
     * 当前消费消息的 tag，可以为空。
     */
    private final String tag;

    /**
     * 当前消费消息的业务 key，可以为空。
     */
    private final String key;

    /**
     * 当前消息已被 RocketMQ 重试的次数。
     */
    private final int reconsumeTimes;

    /**
     * 发送端透传到消费端的业务 headers。
     */
    private final Map<String, String> headers;

    /**
     * 创建消费上下文。
     *
     * @param messageId 消息唯一标识
     * @param topic 当前消息 topic
     * @param tag 当前消息 tag
     * @param key 当前消息业务 key
     * @param reconsumeTimes 当前重试次数
     * @param headers 透传业务 headers
     */
    public MqConsumeContext(String messageId, String topic, String tag, String key, int reconsumeTimes, Map<String, String> headers) {
        this.messageId = messageId;
        this.topic = topic;
        this.tag = tag;
        this.key = key;
        this.reconsumeTimes = reconsumeTimes;
        this.headers = Collections.unmodifiableMap(headers == null ? new LinkedHashMap<String, String>() : new LinkedHashMap<String, String>(headers));
    }

    public String getMessageId() {
        return messageId;
    }

    public String getTopic() {
        return topic;
    }

    public String getTag() {
        return tag;
    }

    public String getKey() {
        return key;
    }

    public int getReconsumeTimes() {
        return reconsumeTimes;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
