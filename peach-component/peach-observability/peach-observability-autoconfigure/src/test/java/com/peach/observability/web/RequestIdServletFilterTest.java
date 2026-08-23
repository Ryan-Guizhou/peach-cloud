package com.peach.observability.web;

import com.peach.observability.config.PeachObservabilityProperties;
import com.peach.observability.core.ObservabilityConstants;
import com.peach.observability.core.RequestIdResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdServletFilterTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void shouldExposeRequestIdAndRestoreMdc() throws Exception {
        PeachObservabilityProperties.RequestId properties = new PeachObservabilityProperties.RequestId();
        RequestIdResolver resolver = new RequestIdResolver(properties, () -> "generated-request-id");
        RequestIdServletFilter filter = new RequestIdServletFilter(properties, resolver);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(properties.getHeaderName(), "upstream-request-id");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MDC.put(ObservabilityConstants.REQUEST_ID_MDC_KEY, "previous-request-id");

        filter.doFilter(request, response, (actualRequest, actualResponse) -> {
            assertThat(MDC.get(ObservabilityConstants.REQUEST_ID_MDC_KEY)).isEqualTo("upstream-request-id");
            assertThat(actualRequest.getAttribute(ObservabilityConstants.REQUEST_ID_ATTRIBUTE))
                    .isEqualTo("upstream-request-id");
        });

        assertThat(response.getHeader(properties.getHeaderName())).isEqualTo("upstream-request-id");
        assertThat(MDC.get(ObservabilityConstants.REQUEST_ID_MDC_KEY)).isEqualTo("previous-request-id");
    }
}
