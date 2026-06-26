package com.peach.rocket.route;

/**
 * MQ 发送路由。
 *
 * <p>该模型表示一次消息发送在路由解析后的最终结果。发送端会根据这里的 topic、tag 和 key 继续构建
 * 标准消息信封，并调用 RocketMQ 原生 API 完成投递。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
public class MqRoute {

    /**
     * 最终发送 topic。
     */
    private final String topic;

    /**
     * 最终发送 tag，可以为空。
     */
    private final String tag;

    /**
     * 最终发送业务 key，可以为空。
     */
    private final String key;


    public MqRoute(String topic, String tag, String key) {
        this.topic = topic;
        this.tag = tag;
        this.key = key;
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
}
