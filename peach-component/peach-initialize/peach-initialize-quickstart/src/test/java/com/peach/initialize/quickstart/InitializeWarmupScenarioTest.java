package com.peach.initialize.quickstart;

import com.peach.initialize.quickstart.scenario.InitializeWarmupScenarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 ApplicationStarted 阶段预热处理器已执行。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@SpringBootTest(properties = "quickstart.initialize.demo.enabled=false")
class InitializeWarmupScenarioTest {

    @Autowired
    private InitializeWarmupScenarioService scenarioService;

    @Test
    void shouldWarmUpDuringApplicationStarted() {
        assertThat(scenarioService.isWarmedUp()).isTrue();
        assertThat(scenarioService.getWarmupPayload()).isEqualTo("cache-ready");
    }
}
