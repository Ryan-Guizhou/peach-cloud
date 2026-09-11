package com.peach.observability.quickstart.example;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

/**
 * 使用 starter 引入的 {@link Tracer} 创建本地 Span。采样关闭时 Span 为 noop，不导出 OTLP。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:00
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class LocalSpanExample {

    private final Tracer tracer;

    /**
     * 开启并结束一个本地演示 Span。
     *
     * @return 当前 Span 的 traceId；采样关闭时仍可能为空或全零
     */
    public String startAndFinish() {
        Span span = tracer.nextSpan().name("qs-observability-demo").start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            String traceId = span.context().traceId();
            log.info("local span finished, noop={}, traceIdPresent={}",
                    span.isNoop(), traceId != null && !traceId.isBlank());
            return traceId;
        } finally {
            span.end();
        }
    }
}
