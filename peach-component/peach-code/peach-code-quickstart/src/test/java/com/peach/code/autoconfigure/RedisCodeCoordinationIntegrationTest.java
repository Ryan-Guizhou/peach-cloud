package com.peach.code.autoconfigure;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 通过真实 Redis 执行 Lua，验证丢失恢复、超前水位、精度和溢出。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/23 11:10
 */
@Testcontainers(disabledWithoutDocker = true)
class RedisCodeCoordinationIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379);

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redis;
    private RedisCodeSequenceCoordinator coordinator;

    @BeforeEach
    void setup() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        redis = new StringRedisTemplate(connectionFactory);
        coordinator = new RedisCodeSequenceCoordinator(redis, new CodeProperties());
        // 仅测试独占容器允许扫描测试命名空间。
        Set<String> keys = redis.keys("peach:code:sequence:v2:*");
        if (keys != null && !keys.isEmpty()) redis.delete(keys);
    }

    @AfterEach
    void close() {
        connectionFactory.destroy();
    }

    @Test
    void lossBehindAndAheadStatesReconcileWithoutReusingDurableNumbers() {
        assertThat(coordinator.next("T001", "MENU", 41, 99999)).isEqualTo(42);
        assertThat(coordinator.next("T001", "MENU", 0, 99999)).isEqualTo(43);
        assertThat(coordinator.next("T002", "MENU", 0, 99999)).isEqualTo(1);
        assertThat(coordinator.next("T001", "ROLE", 0, 99999)).isEqualTo(1);
        redis.delete(redis.keys("peach:code:sequence:v2:*"));
        assertThat(coordinator.next("T001", "MENU", 43, 99999)).isEqualTo(44);
        assertThat(coordinator.next("T001", "MENU", 100, 99999)).isEqualTo(101);
    }

    @Test
    void eighteenDigitRedisSequenceIsExactAndOverflowIsRejected() {
        assertThat(coordinator.next("T001", "MENU", 999999999999999997L, 999999999999999999L))
                .isEqualTo(999999999999999998L);
        assertThat(coordinator.next("T001", "MENU", 999999999999999998L, 999999999999999999L))
                .isEqualTo(999999999999999999L);
        assertThat(coordinator.next("T001", "MENU", 999999999999999998L, 999999999999999999L))
                .isEqualTo(-1);
    }
}
