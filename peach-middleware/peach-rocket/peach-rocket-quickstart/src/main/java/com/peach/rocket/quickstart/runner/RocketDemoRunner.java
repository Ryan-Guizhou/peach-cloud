package com.peach.rocket.quickstart.runner;

import com.peach.rocket.core.MqSendResult;
import com.peach.rocket.quickstart.example.ConsumeIdempotentExample;
import com.peach.rocket.quickstart.example.OrderOrderlyPublishExample;
import com.peach.rocket.quickstart.example.OrderPublishConsumeExample;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.util.UUID;

/**
 * 启动后演示订单发布-消费、顺序发布与内存消费幂等。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 19:00
 */
@Slf4j
@Indexed
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "quickstart.rocket.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RocketDemoRunner implements ApplicationRunner {

    private final OrderPublishConsumeExample publishConsumeExample;
    private final OrderOrderlyPublishExample orderlyPublishExample;
    private final ConsumeIdempotentExample idempotentExample;

    @Override
    public void run(ApplicationArguments args) {
        runPublishConsumeDemo();
        runOrderlyDemo();
        runIdempotentDemo();
        log.info("rocket demo finished");
    }

    private void runPublishConsumeDemo() {
        log.info("=== order publish-consume demo ===");
        MqSendResult result = publishConsumeExample.publishCreated(10001L);
        log.info("publish-consume success={}, consumed={}",
                result.isSuccess(), publishConsumeExample.consumedCreated().size());
    }

    private void runOrderlyDemo() {
        log.info("=== order orderly publish demo ===");
        MqSendResult result = orderlyPublishExample.publishPaidOrderly(10001L);
        log.info("orderly success={}, shardingKeys={}, consumed={}",
                result.isSuccess(), orderlyPublishExample.orderlyShardingKeys(),
                orderlyPublishExample.consumedPaid().size());
    }

    private void runIdempotentDemo() {
        log.info("=== consume idempotent demo ===");
        int processed = idempotentExample.consumeTwice("qs:rocket:idem:" + UUID.randomUUID());
        log.info("idempotent processed={}", processed);
    }
}
