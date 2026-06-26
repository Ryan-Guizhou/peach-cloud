package com.peach.rocket.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 单次 MQ 发送参数。
 *
 * <p>该对象用于描述一次发送行为的临时覆盖项，优先级高于事件注解中的默认声明。适合在业务侧按场景动态指定
 * topic、tag、key、顺序消息分片键、超时时间、延迟级别以及透传 headers。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
public class MqSendOptions {

    /**
     * 覆盖默认路由的 topic。
     */
    private String topic;

    /**
     * 覆盖默认路由的 tag。
     */
    private String tag;

    /**
     * 覆盖默认路由的业务 key。
     */
    private String key;

    /**
     * 顺序消息分片键，相同值的消息会路由到同一队列。
     */
    private String shardingKey;

    /**
     * 单次发送超时时间，单位毫秒，默认 3000。
     */
    private long timeoutMillis;

    /**
     * 延迟消息配置，为空表示按普通消息发送。
     */
    private MqDelay delay;

    /**
     * 随消息一起透传的业务 headers。
     */
    private Map<String, String> headers = new LinkedHashMap<String, String>();

    public static MqSendOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
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

    public String getShardingKey() {
        return shardingKey;
    }

    public void setShardingKey(String shardingKey) {
        this.shardingKey = shardingKey;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public MqDelay getDelay() {
        return delay;
    }

    public void setDelay(MqDelay delay) {
        this.delay = delay;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers == null ? new LinkedHashMap<String, String>() : headers;
    }

    public static class Builder {

        private final MqSendOptions options = new MqSendOptions();

        private Builder() {
            options.setTimeoutMillis(3000L);
        }

        public Builder topic(String topic) {
            options.setTopic(topic);
            return this;
        }

        public Builder tag(String tag) {
            options.setTag(tag);
            return this;
        }

        public Builder key(String key) {
            options.setKey(key);
            return this;
        }

        public Builder shardingKey(String shardingKey) {
            options.setShardingKey(shardingKey);
            return this;
        }

        public Builder timeoutMillis(long timeoutMillis) {
            options.setTimeoutMillis(timeoutMillis);
            return this;
        }

        public Builder delay(MqDelay delay) {
            options.setDelay(delay);
            return this;
        }

        public Builder header(String name, String value) {
            options.headers.put(name, value);
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            options.headers.putAll(headers == null ? Collections.<String, String>emptyMap() : headers);
            return this;
        }

        public MqSendOptions build() {
            return options;
        }
    }
}
