package com.peach.rocket.context;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultMqHeaderResolverTest {

    @Test
    void shouldCopyBusinessHeadersAndInjectTraceContext() {
        AtomicBoolean injected = new AtomicBoolean();
        MqTraceContextPropagator propagator = new MqTraceContextPropagator() {
            @Override
            public void inject(Map<String, String> headers) {
                headers.put("traceparent", "trace-value");
                injected.set(true);
            }

            @Override
            public MqTraceScope startConsumerSpan(String topic, Map<String, String> headers) {
                return MqTraceScope.NOOP;
            }
        };
        DefaultMqHeaderResolver resolver = new DefaultMqHeaderResolver(propagator);
        Map<String, String> source = new LinkedHashMap<>();
        source.put("tenant", "tenant-a");

        Map<String, String> resolved = resolver.resolve(source);

        assertThat(injected).isTrue();
        assertThat(resolved).containsEntry("tenant", "tenant-a")
                .containsEntry("traceparent", "trace-value");
        assertThat(source).doesNotContainKey("traceparent");
    }
}
