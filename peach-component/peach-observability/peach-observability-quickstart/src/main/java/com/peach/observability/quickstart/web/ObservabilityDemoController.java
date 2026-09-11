package com.peach.observability.quickstart.web;

import com.peach.observability.quickstart.scenario.ObservabilityScenarioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 可观测性演示 REST：返回当前 RequestId。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@RestController
@RequestMapping("/demo")
public class ObservabilityDemoController {

    private final ObservabilityScenarioService scenarioService;

    /**
     * @param scenarioService 场景服务
     */
    public ObservabilityDemoController(ObservabilityScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    /**
     * @return ping 响应
     */
    @GetMapping("/ping")
    public Map<String, String> ping() {
        return scenarioService.ping();
    }
}
