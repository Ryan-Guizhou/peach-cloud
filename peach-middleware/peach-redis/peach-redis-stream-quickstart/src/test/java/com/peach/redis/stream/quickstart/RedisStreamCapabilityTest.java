package com.peach.redis.stream.quickstart;

import com.peach.redis.stream.quickstart.example.OrderStreamExample;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 验证 Redis Stream：push/consume 与同 RecordId 幂等去重。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:50
 */
@SpringBootTest(properties = "quickstart.stream.demo.enabled=false")
@Testcontainers(disabledWithoutDocker = true)
class RedisStreamCapabilityTest {

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
        registry.add("peach.redis.stream.enable", () -> "true");
        registry.add("peach.redis.stream.stream-name", () -> "order-status");
        registry.add("peach.redis.stream.consumer-group", () -> "order-status-group");
        registry.add("peach.redis.stream.consumer-name", () -> "order-status-consumer");
        registry.add("peach.redis.stream.consumer-type", () -> "group");
    }

    @Autowired
    private OrderStreamExample orderStreamExample;

    @Test
    void shouldPushAndConsumeOrderStatus() {
        String payload = orderStreamExample.pushAndConsume("ord-test-1", "SHIPPED");
        assertThat(payload).contains("ord-test-1").contains("SHIPPED");
    }

    @Test
    void shouldSkipDuplicateRecordId() {
        assertThatCode(() -> orderStreamExample.pushThenReplayDuplicate("ord-test-dup", "PAID"))
                .doesNotThrowAnyException();
    }
}
