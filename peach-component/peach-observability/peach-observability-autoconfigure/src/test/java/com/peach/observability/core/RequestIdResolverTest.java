package com.peach.observability.core;

import com.peach.observability.config.PeachObservabilityProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 请求IdResolverTest。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */

class RequestIdResolverTest {

    @Test
    void shouldReuseTrustedValidRequestId() {
        PeachObservabilityProperties.RequestId properties = new PeachObservabilityProperties.RequestId();
        RequestIdResolver resolver = new RequestIdResolver(properties, () -> "generated-request-id");

        assertThat(resolver.resolve(" upstream_123 ")).isEqualTo("upstream_123");
    }

    @Test
    void shouldGenerateRequestIdWhenIncomingValueIsUntrusted() {
        PeachObservabilityProperties.RequestId properties = new PeachObservabilityProperties.RequestId();
        properties.setTrustIncoming(false);
        RequestIdResolver resolver = new RequestIdResolver(properties, () -> "generated-request-id");

        assertThat(resolver.resolve("upstream_123")).isEqualTo("generated-request-id");
    }

    @Test
    void shouldRejectGeneratorWithUnsafeValue() {
        PeachObservabilityProperties.RequestId properties = new PeachObservabilityProperties.RequestId();
        RequestIdResolver resolver = new RequestIdResolver(properties, () -> "bad\r\nvalue");

        assertThatThrownBy(() -> resolver.resolve(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("RequestIdGenerator returned an invalid request ID");
    }
}
