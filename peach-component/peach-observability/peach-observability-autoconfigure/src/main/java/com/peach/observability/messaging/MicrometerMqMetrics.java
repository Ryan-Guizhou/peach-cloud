package com.peach.observability.messaging;

import com.peach.rocket.metrics.MqMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import org.springframework.util.StringUtils;

/**
 * RocketMQ 低基数业务指标适配，绝不添加 messageId、key、原始 Topic 等动态标签。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13
 */
public final class MicrometerMqMetrics implements MqMetrics {

    private final MeterRegistry registry;

    public MicrometerMqMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void publish(String mode, String outcome, long durationNanos) {
        registry.counter("peach.rocket.publish.total",
                "mode", safeMode(mode), "outcome", safeOutcome(outcome)).increment();
        Timer.builder("peach.rocket.publish.duration")
                .tags("mode", safeMode(mode), "outcome", safeOutcome(outcome))
                .register(registry).record(Math.max(0L, durationNanos), TimeUnit.NANOSECONDS);
    }

    @Override
    public void consume(String outcome, long durationNanos) {
        registry.counter("peach.rocket.consume.total", "outcome", safeOutcome(outcome)).increment();
        Timer.builder("peach.rocket.consume.duration")
                .tag("outcome", safeOutcome(outcome))
                .register(registry).record(Math.max(0L, durationNanos), TimeUnit.NANOSECONDS);
    }

    @Override
    public void outbox(String outcome) {
        registry.counter("peach.rocket.outbox.total", "outcome", safeOutcome(outcome)).increment();
    }

    @Override
    public void backpressure() {
        registry.counter("peach.rocket.consumer.backpressure.total").increment();
    }

    private static String safeMode(String value) {
        return "sync".equals(value) || "async".equals(value) || "oneway".equals(value)
                ? value : "other";
    }

    private static String safeOutcome(String value) {
        return switch (value) {
            case "success", "rejected", "failed", "submitted",
                    "sent", "send_failed", "ack_failed" -> value;
            default -> "other";
        };
    }
}
