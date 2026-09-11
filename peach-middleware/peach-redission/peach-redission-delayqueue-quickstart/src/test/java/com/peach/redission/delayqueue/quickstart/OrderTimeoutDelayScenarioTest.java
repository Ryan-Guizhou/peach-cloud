package com.peach.redission.delayqueue.quickstart;

import com.peach.redission.delayqueue.quickstart.scenario.OrderTimeoutConsumerTask;
import com.peach.redission.delayqueue.quickstart.scenario.OrderTimeoutDelayScenarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 基于 Testcontainers Redis 验证短延迟订单超时消息消费。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@SpringBootTest(properties = "quickstart.delayqueue.demo.enabled=false")
@Testcontainers(disabledWithoutDocker = true)
class OrderTimeoutDelayScenarioTest {

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("peach.redis.mode", () -> "standalone");
        registry.add("peach.redis.host", () -> REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
        registry.add("peach.redis.password", () -> "");
        registry.add("peach.redis.database", () -> "0");
        registry.add("peach.delay.queue.isolation-region-count", () -> "1");
    }

    @Autowired
    private OrderTimeoutDelayScenarioService scenarioService;

    @Autowired
    private OrderTimeoutConsumerTask consumerTask;

    @Test
    void shouldConsumeDelayedOrderTimeoutMessage() throws InterruptedException {
        scenarioService.sendTimeoutMessage("it-ord", 1L);
        boolean consumed = consumerTask.getLatch().await(20, TimeUnit.SECONDS);
        assertThat(consumed).isTrue();
        assertThat(consumerTask.getLastContent()).contains("it-ord");
    }
}
