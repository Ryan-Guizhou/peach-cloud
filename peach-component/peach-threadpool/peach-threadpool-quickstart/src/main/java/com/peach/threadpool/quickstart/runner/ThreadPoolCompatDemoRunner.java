package com.peach.threadpool.quickstart.runner;

import com.peach.threadpool.quickstart.scenario.ThreadPoolCompatScenarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 兼容演示：仅调用一次旧 ThreadPoolManager API。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@Component
@ConditionalOnProperty(prefix = "quickstart.threadpool.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ThreadPoolCompatDemoRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ThreadPoolCompatDemoRunner.class);

    private final ThreadPoolCompatScenarioService scenarioService;

    /**
     * @param scenarioService 兼容场景
     */
    public ThreadPoolCompatDemoRunner(ThreadPoolCompatScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String result = scenarioService.runOnce();
        log.info("threadpool compat quickstart finished, result={}", result);
    }
}
