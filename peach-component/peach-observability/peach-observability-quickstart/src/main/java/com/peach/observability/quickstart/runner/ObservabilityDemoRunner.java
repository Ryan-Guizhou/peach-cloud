package com.peach.observability.quickstart.runner;

import com.peach.observability.quickstart.example.CustomMetricExample;
import com.peach.observability.quickstart.example.LocalSpanExample;
import com.peach.observability.quickstart.example.RequestIdResolveExample;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

/**
 * 启动后演示 RequestIdResolver、自定义 Micrometer 指标与本地 Tracer Span。
 *
 * <p>Servlet 过滤器需 HTTP 请求触发，见 {@code GET /demo/ping}。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:00
 */
@Slf4j
@Indexed
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "quickstart.observability.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ObservabilityDemoRunner implements ApplicationRunner {

    private static final String TRUSTED_REQUEST_ID = "trusted-request-id-01";

    private final RequestIdResolveExample requestIdResolveExample;
    private final CustomMetricExample customMetricExample;
    private final LocalSpanExample localSpanExample;

    @Override
    public void run(ApplicationArguments args) {
        runRequestIdDemo();
        runMetricDemo();
        runSpanDemo();
        log.info("observability demo finished");
    }

    private void runRequestIdDemo() {
        log.info("=== RequestIdResolver demo ===");
        String trusted = requestIdResolveExample.resolve(TRUSTED_REQUEST_ID);
        String generated = requestIdResolveExample.resolve("bad id!");
        if (!TRUSTED_REQUEST_ID.equals(trusted) || !requestIdResolveExample.isValid(generated)
                || generated.equals("bad id!")) {
            throw new IllegalStateException("requestId resolve demo failed");
        }
        log.info("requestId demo ok, trustedReused=true, generatedLength={}", generated.length());
    }

    private void runMetricDemo() {
        log.info("=== Custom Micrometer Counter demo ===");
        double first = customMetricExample.incrementHits();
        double second = customMetricExample.incrementHits();
        if (second < first + 1.0) {
            throw new IllegalStateException("custom metric demo failed");
        }
        log.info("metric demo ok, first={}, second={}", first, second);
    }

    private void runSpanDemo() {
        log.info("=== Local Tracer span demo ===");
        localSpanExample.startAndFinish();
        log.info("span demo ok, tip=sampling is off so span may be noop");
    }
}
