package com.peach.redission.delayqueue.quickstart;

import com.peach.redission.delayqueue.quickstart.example.OrderTimeoutDelayExample;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证延迟队列：单消息与多消息消费确认。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:05
 */
@SpringBootTest(properties = "quickstart.delayqueue.demo.enabled=false")
@Testcontainers(disabledWithoutDocker = true)
class DelayQueueCapabilityTest {

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
        registry.add("peach.delay.queue.use-reliable-queue", () -> "true");
    }

    @Autowired
    private OrderTimeoutDelayExample orderTimeoutDelayExample;

    @Test
    void shouldSendAndConsumeSingleMessage() {
        String content = orderTimeoutDelayExample.sendAndAwait("ord-test-1");
        assertThat(content).contains("ord-test-1");
    }

    @Test
    void shouldSendAndConsumeMultipleMessages() {
        List<String> received = orderTimeoutDelayExample.sendMultipleAndAwait("ord-test-multi", 3);
        assertThat(received).hasSize(3);
        assertThat(received).anyMatch(s -> s.contains("ord-test-multi-1"));
        assertThat(received).anyMatch(s -> s.contains("ord-test-multi-2"));
        assertThat(received).anyMatch(s -> s.contains("ord-test-multi-3"));
    }
}
