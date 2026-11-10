package com.peach.satoken.quickstart;

import cn.dev33.satoken.dao.SaTokenDao;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peach.satoken.dao.PeachSaTokenDao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Redis 7.2 上验证 Sa-Token DAO 真实持久化与 HTTP 登录、鉴权和退出闭环。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@EnabledIfEnvironmentVariable(named = "RUN_REDIS_CLIENT_IT", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "quickstart.satoken.demo.enabled=false",
        "quickstart.satoken.demo.password=satoken-redis-integration",
        "peach.satoken.dao.enabled=true",
        "peach.redis.enabled=true",
        "peach.redis.mode=standalone",
        "peach.redis.host=127.0.0.1:6379",
        "peach.redis.database=0",
        "peach.redis.password="
})
@AutoConfigureMockMvc
class SaTokenRedisServerIntegrationTest {

    @Autowired
    private SaTokenDao persistence;

    @Autowired
    private RedisConnectionFactory connectionFactory;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void shouldLoginPersistVerifyLogoutAndRejectUsingRedisDao() throws Exception {
        assertThat(persistence).isInstanceOf(PeachSaTokenDao.class);
        assertThat(connectionFactory).isInstanceOf(JedisConnectionFactory.class);

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo\",\"password\":\"satoken-redis-integration\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenValue").isNotEmpty())
                .andReturn();
        JsonNode data = json.readTree(result.getResponse().getContentAsString());
        String name = data.get("tokenName").asText();
        String value = data.get("tokenValue").asText();
        assertThat(value).isNotBlank();

        mockMvc.perform(get("/api/profile").header(name, value))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginId").value("demo"));

        mockMvc.perform(post("/auth/logout").header(name, value))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/profile").header(name, value))
                .andExpect(status().isUnauthorized());
    }
}
