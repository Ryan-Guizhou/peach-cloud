package com.peach.observability.quickstart;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证 RequestId 过滤器与 ping 接口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "quickstart.observability.demo.enabled=false")
@AutoConfigureMockMvc
class ObservabilityScenarioTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldEchoTrustedRequestId() throws Exception {
        mockMvc.perform(get("/demo/ping").header("X-Request-Id", "trusted-request-id-01"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "trusted-request-id-01"))
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.requestId").value("trusted-request-id-01"));
    }

    @Test
    void shouldExposeActuatorHealth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
