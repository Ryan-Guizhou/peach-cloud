package com.peach.rocket.metrics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 监控采集失败不影响业务 ACK、发送和重试语义。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13
 */
class MqMetricsGuardTest {

    @Test
    void shouldSuppressMetricsFailureWithoutChangingDeliveryOutcome() {
        MqMetrics delegate = mock(MqMetrics.class);
        doThrow(new IllegalStateException("registry unavailable"))
                .when(delegate).consume(eq("success"), anyLong());
        MqMetrics safe = MqMetricsGuard.of(delegate);

        assertDoesNotThrow(() -> safe.consume("success", 100L));
        assertDoesNotThrow(() -> safe.publish("sync", "success", 200L));
        assertDoesNotThrow(() -> safe.outbox("sent"));
        assertDoesNotThrow(safe::backpressure);
        verify(delegate).publish("sync", "success", 200L);
    }

    @Test
    void shouldReuseNoopAndExistingGuard() {
        assertSame(MqMetrics.NOOP, MqMetricsGuard.of(null));
        assertSame(MqMetrics.NOOP, MqMetricsGuard.of(MqMetrics.NOOP));
        MqMetrics safe = MqMetricsGuard.of(mock(MqMetrics.class));
        assertSame(safe, MqMetricsGuard.of(safe));
    }
}
