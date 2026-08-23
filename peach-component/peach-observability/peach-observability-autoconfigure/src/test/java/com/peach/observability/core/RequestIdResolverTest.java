package com.peach.observability.core;

import com.peach.observability.config.PeachObservabilityProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
