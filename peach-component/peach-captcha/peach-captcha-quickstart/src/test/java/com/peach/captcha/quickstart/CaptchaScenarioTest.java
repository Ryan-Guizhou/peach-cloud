package com.peach.captcha.quickstart;

import com.peach.captcha.model.CaptchaVO;
import com.peach.captcha.quickstart.scenario.CaptchaScenarioService;
import com.peach.common.response.Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证验证码生成 REST 与服务入口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "quickstart.captcha.demo.enabled=false")
@AutoConfigureMockMvc
class CaptchaScenarioTest {

    @Autowired
    private CaptchaScenarioService scenarioService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldGenerateCaptchaViaService() {
        Response response = scenarioService.getCaptcha("it-client");
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isInstanceOf(CaptchaVO.class);
        CaptchaVO captchaVO = (CaptchaVO) response.getData();
        assertThat(captchaVO.getToken()).isNotBlank();
        assertThat(captchaVO.getSlidingOriginalImageBase64()).isNotBlank();
    }

    @Test
    void shouldExposeGetEndpoint() throws Exception {
        mockMvc.perform(post("/captcha/get")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientUid\":\"mvc-client\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }
}
