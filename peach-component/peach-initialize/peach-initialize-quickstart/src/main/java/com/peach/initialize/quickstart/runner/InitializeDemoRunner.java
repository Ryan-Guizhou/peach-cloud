package com.peach.initialize.quickstart.runner;

import com.peach.initialize.quickstart.scenario.InitializeWarmupScenarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 启动后打印预热结果。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@Component
@ConditionalOnProperty(prefix = "quickstart.initialize.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InitializeDemoRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InitializeDemoRunner.class);

    private final InitializeWarmupScenarioService scenarioService;

    /**
     * @param scenarioService 预热场景服务
     */
    public InitializeDemoRunner(InitializeWarmupScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("initialize quickstart finished, warmedUp={}, payload={}",
                scenarioService.isWarmedUp(), scenarioService.getWarmupPayload());
    }
}
