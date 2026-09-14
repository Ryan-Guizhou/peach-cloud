package com.peach.redission.delayqueue.quickstart.runner;

import com.peach.redission.delayqueue.quickstart.example.OrderTimeoutDelayExample;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * 启动后演示延迟投递与多消息消费确认。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:05
 */
@Slf4j
@Indexed
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "quickstart.delayqueue.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DelayQueueDemoRunner implements ApplicationRunner {

    private final OrderTimeoutDelayExample orderTimeoutDelayExample;

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== DelayQueue single message demo ===");
        String one = orderTimeoutDelayExample.sendAndAwait("ord-demo-1");
        log.info("consumed={}", one);

        log.info("=== DelayQueue multi message demo ===");
        List<String> many = orderTimeoutDelayExample.sendMultipleAndAwait("ord-demo-multi", 3);
        log.info("consumed size={}", many.size());
        log.info("delayqueue demo finished");
    }
}
