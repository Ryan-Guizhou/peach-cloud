package com.peach.redission.repeat.quickstart.runner;

import com.peach.redission.repeat.quickstart.scenario.OrderSubmitRepeatScenarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 启动后演示防重复提交：第二次调用应被拦截。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@Component
@ConditionalOnProperty(prefix = "quickstart.repeat.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RepeatDemoRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RepeatDemoRunner.class);

    private final OrderSubmitRepeatScenarioService scenarioService;

    /**
     * @param scenarioService 防重复场景服务
     */
    public RepeatDemoRunner(OrderSubmitRepeatScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    @Override
    public void run(ApplicationArguments args) {
        String requestId = "req-5001";
        String first = scenarioService.submitOrder(requestId);
        try {
            scenarioService.submitOrder(requestId);
            log.warn("repeat quickstart unexpected: second submit was accepted");
        } catch (IllegalStateException ex) {
            log.info("repeat quickstart finished, first={}, secondBlocked={}", first, ex.getMessage());
        }
    }
}
