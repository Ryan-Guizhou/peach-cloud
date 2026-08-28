package com.peach.rocket.context;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 默认MQHeaderResolverTest。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */

class DefaultMqHeaderResolverTest {

    @Test
    void shouldCopyBusinessHeadersAndInjectTraceContext() {
        AtomicBoolean injected = new AtomicBoolean();
        MqTraceContextPropagator propagator = new TraceInjectingPropagator(injected);
        DefaultMqHeaderResolver resolver = new DefaultMqHeaderResolver(propagator);
        Map<String, String> source = new LinkedHashMap<>();
        source.put("tenant", "tenant-a");

        Map<String, String> resolved = resolver.resolve(source);

        assertThat(injected).isTrue();
        assertThat(resolved).containsEntry("tenant", "tenant-a")
                .containsEntry("traceparent", "trace-value");
        assertThat(source).doesNotContainKey("traceparent");
    }

    /**
     * TraceInjectingPropagator。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */

    private static final class TraceInjectingPropagator implements MqTraceContextPropagator {

        private final AtomicBoolean injected;

        private TraceInjectingPropagator(AtomicBoolean injected) {
            this.injected = injected;
        }

        @Override
        public void inject(Map<String, String> headers) {
            headers.put("traceparent", "trace-value");
            injected.set(true);
        }

        @Override
        public MqTraceScope startConsumerSpan(String topic, Map<String, String> headers) {
            return MqTraceScope.NOOP;
        }
    }
}
