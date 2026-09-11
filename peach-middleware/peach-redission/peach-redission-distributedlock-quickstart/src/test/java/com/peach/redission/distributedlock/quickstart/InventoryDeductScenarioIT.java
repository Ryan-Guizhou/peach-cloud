package com.peach.redission.distributedlock.quickstart;

import com.peach.redission.distributedlock.quickstart.scenario.InventoryDeductScenarioService;
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

/**
 * 基于 Testcontainers Redis 验证分布式锁库存扣减。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@SpringBootTest(properties = "quickstart.distributedlock.demo.enabled=false")
@Testcontainers(disabledWithoutDocker = true)
class InventoryDeductScenarioIT {

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
    private InventoryDeductScenarioService scenarioService;

    @Test
    void shouldDeductStockUnderDistributedLock() {
        String skuId = "it-sku";
        scenarioService.resetStock(skuId, 5);
        assertThat(scenarioService.deduct(skuId, 2)).isEqualTo(3);
        assertThat(scenarioService.getStock(skuId)).isEqualTo(3);
    }
}
