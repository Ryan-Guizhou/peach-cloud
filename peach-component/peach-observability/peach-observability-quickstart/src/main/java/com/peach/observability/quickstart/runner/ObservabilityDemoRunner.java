package com.peach.observability.quickstart.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 启动后提示 RequestId 与 Actuator 入口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@Component
@ConditionalOnProperty(prefix = "quickstart.observability.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ObservabilityDemoRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ObservabilityDemoRunner.class);

    @Override
    public void run(ApplicationArguments args) {
        log.info("observability quickstart ready: GET /demo/ping returns requestId; actuator health at /actuator/health");
    }
}
