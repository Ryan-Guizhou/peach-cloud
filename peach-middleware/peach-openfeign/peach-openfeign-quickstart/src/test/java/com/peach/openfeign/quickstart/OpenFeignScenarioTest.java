package com.peach.openfeign.quickstart;

import com.peach.openfeign.quickstart.scenario.OpenFeignScenarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 Feign 调用本机 Stub。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "quickstart.openfeign.demo.enabled=false",
                "quickstart.openfeign.downstream-url=http://127.0.0.1:${local.server.port}"
        })
class OpenFeignScenarioTest {

    @Autowired
    private OpenFeignScenarioService scenarioService;

    @Test
    void shouldCallLocalStubViaFeign() {
        Map<String, String> response = scenarioService.echoViaFeign("it-message");
        assertThat(response.get("source")).isEqualTo("stub");
        assertThat(response.get("message")).isEqualTo("it-message");
    }
}
