package com.peach.redission.delayqueue.quickstart.runner;

import com.peach.redission.delayqueue.quickstart.scenario.OrderTimeoutDelayScenarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 启动后演示订单超时延迟消息投递。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@Component
@ConditionalOnProperty(prefix = "quickstart.delayqueue.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DelayQueueDemoRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DelayQueueDemoRunner.class);

    private final OrderTimeoutDelayScenarioService scenarioService;

    /**
     * @param scenarioService 延迟队列场景服务
     */
    public DelayQueueDemoRunner(OrderTimeoutDelayScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    @Override
    public void run(ApplicationArguments args) {
        scenarioService.sendTimeoutMessage("ord-3001", 1L);
        log.info("delayqueue quickstart finished, sent order-timeout message with 1s delay");
    }
}
