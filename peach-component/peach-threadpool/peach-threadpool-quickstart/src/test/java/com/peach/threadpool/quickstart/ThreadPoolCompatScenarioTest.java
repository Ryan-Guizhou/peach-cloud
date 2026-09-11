package com.peach.threadpool.quickstart;

import com.peach.threadpool.quickstart.scenario.ThreadPoolCompatScenarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证存量 ThreadPoolManager 兼容调用。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@SpringBootTest(properties = "quickstart.threadpool.demo.enabled=false")
class ThreadPoolCompatScenarioTest {

    @Autowired
    private ThreadPoolCompatScenarioService scenarioService;

    @Test
    void shouldSubmitOnceViaLegacyApi() throws Exception {
        assertThat(scenarioService.runOnce()).isEqualTo("compat-ok");
    }
}
