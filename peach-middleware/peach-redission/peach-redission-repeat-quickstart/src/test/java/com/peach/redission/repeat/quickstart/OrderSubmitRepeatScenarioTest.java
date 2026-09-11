package com.peach.redission.repeat.quickstart;

import com.peach.redission.repeat.quickstart.scenario.OrderSubmitRepeatScenarioService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 基于 Testcontainers Redis 验证 @RepeatLimit 第二次调用被拦截。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@SpringBootTest(properties = "quickstart.repeat.demo.enabled=false")
@Testcontainers(disabledWithoutDocker = true)
class OrderSubmitRepeatScenarioTest {

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
    }

    @Autowired
    private OrderSubmitRepeatScenarioService scenarioService;

    @Test
    void shouldBlockDuplicateSubmit() {
        String requestId = "it-req-" + System.nanoTime();
        assertThat(scenarioService.submitOrder(requestId)).startsWith("accepted:");
        assertThatThrownBy(() -> scenarioService.submitOrder(requestId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate");
    }
}
