package com.peach.observability.messaging;

import com.peach.rocket.context.MqTraceContextPropagator;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;

import java.util.Map;

/**
 * 基于 Micrometer Tracing 的 MQ 链路上下文传播器。
 *
 * <p>生产端通过当前 Tracer 和 Propagator 注入标准传播字段；消费端从消息头提取父上下文，
 * 创建 {@link Span.Kind#CONSUMER} Span，并在作用域结束时可靠关闭。</p>
 */
public final class MicrometerMqTraceContextPropagator implements MqTraceContextPropagator {

    private final Tracer tracer;
    private final Propagator propagator;

    /**
     * 创建 Micrometer MQ 链路传播器。
     *
     * @param tracer Micrometer Tracer
     * @param propagator 标准链路传播器
     */
    public MicrometerMqTraceContextPropagator(Tracer tracer, Propagator propagator) {
        this.tracer = tracer;
        this.propagator = propagator;
    }

    @Override
    public void inject(Map<String, String> headers) {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan == null || currentSpan.isNoop()) {
            return;
        }
        propagator.inject(currentSpan.context(), headers, Map::put);
    }

    @Override
    public MqTraceScope startConsumerSpan(String topic, Map<String, String> headers) {
        Map<String, String> carrier = headers == null ? Map.of() : headers;
        Span span = propagator.extract(carrier, Map::get)
                .name("rocketmq consume")
                .kind(Span.Kind.CONSUMER)
                .tag("messaging.system", "rocketmq")
                .tag("messaging.destination.name", safeTopic(topic))
                .start();
        Tracer.SpanInScope spanInScope = tracer.withSpan(span);
        return new MicrometerMqTraceScope(span, spanInScope);
    }

    private static String safeTopic(String topic) {
        return topic == null || topic.isBlank() ? "unknown" : topic;
    }

    private record MicrometerMqTraceScope(Span span, Tracer.SpanInScope spanInScope) implements MqTraceScope {

        @Override
        public void error(Throwable throwable) {
            if (throwable != null) {
                span.error(throwable);
            }
        }

        @Override
        public void close() {
            try {
                spanInScope.close();
            } finally {
                span.end();
            }
        }
    }
}
