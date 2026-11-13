package com.peach.rocket.context;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import org.apache.rocketmq.common.message.MessageConst;
import com.peach.rocket.exception.MqException;
import org.springframework.util.StringUtils;
import org.springframework.util.CollectionUtils;

/**
 * 消息 Header 解析器：防御性拷贝业务字段并注入可选 Trace 上下文。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
public class DefaultMqHeaderResolver {

    private final MqTraceContextPropagator traceContextPropagator;

    /**
     * 创建不启用链路传播的消息头解析器。
     */
    public DefaultMqHeaderResolver() {
        this(MqTraceContextPropagator.NOOP);
    }

    /**
     * 创建支持链路传播的消息头解析器。
     *
     * @param traceContextPropagator MQ 链路上下文传播器
     */
    public DefaultMqHeaderResolver(MqTraceContextPropagator traceContextPropagator) {
        this.traceContextPropagator = traceContextPropagator == null
                ? MqTraceContextPropagator.NOOP
                : traceContextPropagator;
    }

    /**
     * 解析业务传入的消息头。
     *
     * @param headers 业务消息头
     * @return 可写入消息信封的消息头
     */
    public Map<String, String> resolve(Map<String, String> headers) {
        Map<String, String> resolved = CollectionUtils.isEmpty(headers)
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(headers);
        // 上层输入先校验；Trace 插入后的字段还需通过发送准入再次校验。
        for (Map.Entry<String, String> header : resolved.entrySet()) {
            validateBusinessHeader(header.getKey(), header.getValue());
        }
        traceContextPropagator.inject(resolved);
        return resolved;
    }

    /**
     * 统一限制 SDK 保留属性、敏感业务 Header 与非法控制字符。
     *
     * @param name Header 名称
     * @param value Header 内容
     */
    public static void validateBusinessHeader(String name, String value) {
        if (!StringUtils.hasText(name) || value == null) {
            throw new MqException("MQ header key must have text and value must not be null");
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (MessageConst.STRING_HASH_SET.contains(name.toUpperCase(Locale.ROOT))
                || Set.of("id", "timestamp", "peach_rocket_transaction_key").contains(lower)
                || lower.contains("authorization") || lower.contains("password")
                || lower.contains("secret") || lower.contains("token") || lower.contains("cookie")
                || name.chars().anyMatch(Character::isISOControl)
                || value.chars().anyMatch(Character::isISOControl)) {
            throw new MqException("Reserved or sensitive MQ business header is not permitted");
        }
    }

}
