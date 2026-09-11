package com.peach.satoken.quickstart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 在 Web 上下文中验证登录拿 token、鉴权通过、登出后拒绝。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 19:10
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "quickstart.satoken.demo.enabled=false")
@AutoConfigureMockMvc
class SaTokenCapabilityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldLoginAndReturnToken() throws Exception {
        TokenHolder token = login();
        assertThat(token.tokenName()).isNotBlank();
        assertThat(token.tokenValue()).isNotBlank();
        assertThat(token.loginId()).isEqualTo("demo");
    }

    @Test
    void shouldAccessProfileWhenAuthorized() throws Exception {
        TokenHolder token = login();
        mockMvc.perform(get("/api/profile").header(token.tokenName(), token.tokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value(true))
                .andExpect(jsonPath("$.loginId").value("demo"));
    }

    @Test
    void shouldRejectWithoutTokenAndAfterLogout() throws Exception {
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isUnauthorized());

        TokenHolder token = login();
        mockMvc.perform(post("/auth/logout").header(token.tokenName(), token.tokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logout").value(true));

        mockMvc.perform(get("/api/profile").header(token.tokenName(), token.tokenValue()))
                .andExpect(status().isUnauthorized());
    }

    private TokenHolder login() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo\",\"password\":\"demo-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenValue").isNotEmpty())
                .andReturn();
        JsonNode loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return new TokenHolder(
                loginBody.get("tokenName").asText(),
                loginBody.get("tokenValue").asText(),
                loginBody.get("loginId").asText());
    }

    private record TokenHolder(String tokenName, String tokenValue, String loginId) {
    }
}
