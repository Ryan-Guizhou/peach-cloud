package com.peach.openfeign.quickstart.runner;

import com.peach.openfeign.quickstart.scenario.OpenFeignScenarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 启动后通过 Feign 调用本地 Stub。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@Component
@ConditionalOnProperty(prefix = "quickstart.openfeign.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OpenFeignDemoRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OpenFeignDemoRunner.class);

    private final OpenFeignScenarioService scenarioService;

    /**
     * @param scenarioService 场景服务
     */
    public OpenFeignDemoRunner(OpenFeignScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    @Override
    public void run(ApplicationArguments args) {
        Map<String, String> response = scenarioService.echoViaFeign("hello-feign");
        log.info("openfeign quickstart finished, source={}, message={}, requestIdPresent={}",
                response.get("source"),
                response.get("message"),
                response.get("requestId") != null && !response.get("requestId").isBlank());
    }
}
