package com.peach.rocket.context;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 默认MQHeader解析器。
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
        Map<String, String> resolved = headers == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(headers);
        traceContextPropagator.inject(resolved);
        return resolved;
    }
}
