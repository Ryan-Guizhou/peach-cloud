package com.peach.rocket.core;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.CollectionUtils;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RocketMQ 单次发送选项。
 * <p>该对象用于描述一次发送行为的临时覆盖项，优先级高于事件注解中的默认声明。适合在业务侧按场景动态指定
 * topic、tag、key、顺序消息分片键、超时时间、延迟级别以及透传 headers。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
@Getter
@Setter
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

    /**
     * 为每次发送创建参数副本，避免修改业务调用方的原始参数。
     *
     * @param source 业务参数，可为空
     * @return 独立的参数快照
     */
    public static MqSendOptions copyOf(MqSendOptions source) {
        MqSendOptions original = source == null ? defaults() : source;
        MqSendOptions copy = new MqSendOptions();
        copy.topic = original.topic;
        copy.tag = original.tag;
        copy.key = original.key;
        copy.shardingKey = original.shardingKey;
        copy.timeoutMillis = original.timeoutMillis;
        copy.delay = original.delay;
        copy.headers = original.headers == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(original.headers);
        return copy;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 替换消息头时执行防御性拷贝，禁止调用方随后修改本次发送快照。
     *
     * @param headers 业务消息头，可为 null
     */
    public void setHeaders(Map<String, String> headers) {
        this.headers = CollectionUtils.isEmpty(headers) ? new LinkedHashMap<>() : new LinkedHashMap<>(headers);
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
            if (!CollectionUtils.isEmpty(headers)) {
                options.headers.putAll(headers);
            }
            return this;
        }

        public MqSendOptions build() {
            return options;
        }
    }
}
