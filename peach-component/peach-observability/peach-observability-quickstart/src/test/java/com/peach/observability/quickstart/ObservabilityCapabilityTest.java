package com.peach.observability.quickstart;

import com.peach.observability.quickstart.example.CustomMetricExample;
import com.peach.observability.quickstart.example.LocalSpanExample;
import com.peach.observability.quickstart.example.RequestIdResolveExample;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证 RequestIdResolver、Servlet 过滤器、自定义指标与本地 Tracer Span。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:00
 */
@SpringBootTest(properties = "quickstart.observability.demo.enabled=false")
@AutoConfigureMockMvc
class ObservabilityCapabilityTest {

    private static final String TRUSTED_REQUEST_ID = "trusted-request-id-01";

    @Autowired
    private RequestIdResolveExample requestIdResolveExample;

    @Autowired
    private CustomMetricExample customMetricExample;

    @Autowired
    private LocalSpanExample localSpanExample;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void requestIdResolverShouldReuseTrustedAndGenerateOnInvalid() {
        String trusted = requestIdResolveExample.resolve(TRUSTED_REQUEST_ID);
        assertThat(trusted).isEqualTo(TRUSTED_REQUEST_ID);

        String generated = requestIdResolveExample.resolve("bad id!");
        assertThat(generated).isNotEqualTo("bad id!");
        assertThat(requestIdResolveExample.isValid(generated)).isTrue();
    }

    @Test
    void servletFilterShouldEchoTrustedRequestId() throws Exception {
        mockMvc.perform(get("/demo/ping").header("X-Request-Id", TRUSTED_REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", TRUSTED_REQUEST_ID))
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.requestId").value(TRUSTED_REQUEST_ID));
    }

    @Test
    void servletFilterShouldGenerateRequestIdWhenIncomingIsInvalid() throws Exception {
        MvcResult result = mockMvc.perform(get("/demo/ping").header("X-Request-Id", "bad id!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andReturn();

        String responseId = result.getResponse().getHeader("X-Request-Id");
        assertThat(responseId).isNotBlank().isNotEqualTo("bad id!");
        assertThat(requestIdResolveExample.isValid(responseId)).isTrue();
        assertThat(result.getResponse().getContentAsString()).contains(responseId);
    }

    @Test
    void customMetricShouldIncrement() {
        double first = customMetricExample.incrementHits();
        double second = customMetricExample.incrementHits();
        assertThat(second).isGreaterThanOrEqualTo(first + 1.0);
    }

    @Test
    void localSpanShouldStartAndFinish() {
        String traceId = localSpanExample.startAndFinish();
        assertThat(traceId).isNotNull();
    }

    @Test
    void actuatorHealthShouldBeUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
