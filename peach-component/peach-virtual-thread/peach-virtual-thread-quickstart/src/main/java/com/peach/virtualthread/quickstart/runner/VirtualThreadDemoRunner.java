package com.peach.virtualthread.quickstart.runner;

import com.peach.virtualthread.quickstart.VirtualThreadScenarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 启动后演示多业务组并行聚合。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@Component
@ConditionalOnProperty(prefix = "quickstart.virtual-thread.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VirtualThreadDemoRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(VirtualThreadDemoRunner.class);

    private final VirtualThreadScenarioService scenarioService;

    /**
     * @param scenarioService 虚拟线程场景服务
     */
    public VirtualThreadDemoRunner(VirtualThreadScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String result = scenarioService.aggregate().get();
        log.info("virtual-thread quickstart finished, aggregate={}", result);
    }
}
