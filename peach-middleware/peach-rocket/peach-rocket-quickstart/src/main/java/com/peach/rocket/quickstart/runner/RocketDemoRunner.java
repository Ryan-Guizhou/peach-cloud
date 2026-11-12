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
import java.time.Duration;
import java.util.function.BooleanSupplier;

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

    private static final Duration CONSUME_TIMEOUT = Duration.ofSeconds(60);

    private final OrderPublishConsumeExample publishConsumeExample;
    private final OrderOrderlyPublishExample orderlyPublishExample;
    private final ConsumeIdempotentExample idempotentExample;

    @Override
    public void run(ApplicationArguments args) throws InterruptedException {
        long orderId = System.currentTimeMillis();
        runPublishConsumeDemo(orderId);
        runOrderlyDemo(orderId);
        runIdempotentDemo();
        log.info("rocket demo finished");
    }

    private void runPublishConsumeDemo(long orderId) throws InterruptedException {
        log.info("=== order publish-consume demo ===");
        MqSendResult result = publishConsumeExample.publishCreated(orderId);
        awaitConsumption(() -> publishConsumeExample.consumedCreated().stream()
                .anyMatch(event -> event.orderId() == orderId));
        log.info("publish-consume success={}, consumed={}",
                result.isSuccess(), publishConsumeExample.consumedCreated().size());
    }

    private void runOrderlyDemo(long orderId) throws InterruptedException {
        log.info("=== order orderly publish demo ===");
        MqSendResult result = orderlyPublishExample.publishPaidOrderly(orderId);
        awaitConsumption(() -> orderlyPublishExample.consumedPaid().stream()
                .anyMatch(event -> event.orderId() == orderId));
        log.info("orderly success={}, shardingKey={}, consumed={}",
                result.isSuccess(), orderId,
                orderlyPublishExample.consumedPaid().size());
    }

    private void runIdempotentDemo() {
        log.info("=== consume idempotent demo ===");
        int processed = idempotentExample.consumeTwice("qs:rocket:idem:" + UUID.randomUUID());
        log.info("idempotent processed={}", processed);
    }

    private void awaitConsumption(BooleanSupplier consumed) throws InterruptedException {
        long started = System.nanoTime();
        while (!consumed.getAsBoolean()) {
            if (System.nanoTime() - started >= CONSUME_TIMEOUT.toNanos()) {
                throw new IllegalStateException("RocketMQ demo consumption timed out after 60 seconds");
            }
            Thread.sleep(100);
        }
    }
}
