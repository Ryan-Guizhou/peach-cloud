package com.peach.rocket.quickstart;

import com.peach.rocket.core.MqSendResult;
import com.peach.rocket.quickstart.example.OrderOrderlyPublishExample;
import com.peach.rocket.quickstart.example.OrderPublishConsumeExample;
import java.time.Duration;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 真实 RocketMQ Broker 发消息到 Consumer 回调的手动验收测试。
 *
 * <p>缺少 PEACH_ROCKET_BROKER_TEST=true 环境变量时整类跳过，
 * 不得将普通 CI 通过误认为真实 Broker 闭环通过。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13
 */
@EnabledIfEnvironmentVariable(named = "PEACH_ROCKET_BROKER_TEST", matches = "true")
@SpringBootTest(properties = {
        "quickstart.rocket.demo.enabled=false",
        "peach.rocket.topic.auto-create=false",
        "peach.rocket.outbox.enabled=false",
        "peach.rocket.transaction.enabled=false"
})
class RocketBrokerEndToEndTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @Autowired
    private OrderPublishConsumeExample created;

    @Autowired
    private OrderOrderlyPublishExample paid;

    @Test
    void shouldPublishAndConsumeOrdersViaRealBroker() throws InterruptedException {
        long orderId = System.currentTimeMillis();
        MqSendResult createdResult = created.publishCreated(orderId);
        assertTrue(createdResult.isSuccess(), "Broker rejected created event");
        await(() -> created.consumedCreated().stream().anyMatch(e -> e.orderId() == orderId));

        MqSendResult paidResult = paid.publishPaidOrderly(orderId);
        assertTrue(paidResult.isSuccess(), "Broker rejected paid event");
        await(() -> paid.consumedPaid().stream().anyMatch(e -> e.orderId() == orderId));
    }

    private void await(BooleanSupplier condition) throws InterruptedException {
        long started = System.nanoTime();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() - started >= TIMEOUT.toNanos()) {
                throw new AssertionError("RocketMQ Broker consume callback did not finish in 30 seconds");
            }
            Thread.sleep(100);
        }
    }
}
