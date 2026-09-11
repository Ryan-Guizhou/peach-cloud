package com.peach.redission.repeat.quickstart;

import com.peach.redission.repeat.quickstart.example.OrderSubmitRepeatExample;
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
 * 验证 {@code @RepeatLimit}：同 key 二次拒绝，不同 key 互不影响。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:00
 */
@SpringBootTest(properties = "quickstart.repeat.demo.enabled=false")
@Testcontainers(disabledWithoutDocker = true)
class RepeatCapabilityTest {

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
    private OrderSubmitRepeatExample orderSubmitRepeatExample;

    @Test
    void sameKeyShouldRejectSecondSubmit() {
        String requestId = "req-same-" + System.nanoTime();
        assertThat(orderSubmitRepeatExample.submitOrder(requestId)).isEqualTo("accepted:" + requestId);
        assertThatThrownBy(() -> orderSubmitRepeatExample.submitOrder(requestId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate");
    }

    @Test
    void differentKeysShouldNotInterfere() {
        long nano = System.nanoTime();
        String firstId = "req-ind-a-" + nano;
        String secondId = "req-ind-b-" + nano;
        assertThat(orderSubmitRepeatExample.submitOrder(firstId)).isEqualTo("accepted:" + firstId);
        assertThat(orderSubmitRepeatExample.submitOrder(secondId)).isEqualTo("accepted:" + secondId);
    }
}
