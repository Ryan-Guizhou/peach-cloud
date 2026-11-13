package com.peach.rocket.context;

import java.util.LinkedHashMap;
import java.util.Map;
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
        // 在 record 的 Map.copyOf 之前校验用户 Header，避免以 NPE 掩盖输入错误。
        for (Map.Entry<String, String> header : resolved.entrySet()) {
            if (!StringUtils.hasText(header.getKey()) || header.getValue() == null) {
                throw new MqException("MQ header key must have text and value must not be null");
            }
            String name = header.getKey();
            String lower = name.toLowerCase(java.util.Locale.ROOT);
            if (org.apache.rocketmq.common.message.MessageConst.STRING_HASH_SET.contains(name)
                    || java.util.Set.of("id", "timestamp", "peach_rocket_transaction_key").contains(lower)
                    || lower.contains("authorization") || lower.contains("password")
                    || lower.contains("secret") || lower.contains("token") || lower.contains("cookie")
                    || name.chars().anyMatch(Character::isISOControl)
                    || header.getValue().chars().anyMatch(Character::isISOControl)) {
                throw new MqException("Reserved or sensitive MQ business header is not permitted");
            }
        }
        traceContextPropagator.inject(resolved);
        return resolved;
    }
}
