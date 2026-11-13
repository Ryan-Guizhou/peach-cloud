package com.peach.observability.messaging;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 低基数标签和基础发送/消费/Outbox 指标验证。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13
 */
class MicrometerMqMetricsTest {

    @Test
    void shouldRecordCountersAndTimersWithoutDynamicIdentifiers() {
        try (SimpleMeterRegistry registry = new SimpleMeterRegistry()) {
            MicrometerMqMetrics metrics = new MicrometerMqMetrics(registry);
            metrics.publish("sync", "success", 1000L);
            metrics.consume("failed", 2000L);
            metrics.outbox("ack_failed");
            metrics.backpressure();

            assertEquals(1D, registry.get("peach.rocket.publish.total")
                    .tag("mode", "sync").tag("outcome", "success").counter().count());
            assertEquals(1L, registry.get("peach.rocket.consume.duration")
                    .tag("outcome", "failed").timer().count());
            assertEquals(1D, registry.get("peach.rocket.outbox.total")
                    .tag("outcome", "ack_failed").counter().count());
            assertEquals(1D, registry.get("peach.rocket.consumer.backpressure.total").counter().count());
        }
    }

    @Test
    void shouldReduceUnknownValuesToStableLowCardinalityLabels() {
        try (SimpleMeterRegistry registry = new SimpleMeterRegistry()) {
            MicrometerMqMetrics metrics = new MicrometerMqMetrics(registry);
            metrics.publish("dynamic-tenant-123", "dynamic-message-id-abc", 0L);

            assertNotNull(registry.get("peach.rocket.publish.total")
                    .tag("mode", "other").tag("outcome", "other").counter());
        }
    }
}
