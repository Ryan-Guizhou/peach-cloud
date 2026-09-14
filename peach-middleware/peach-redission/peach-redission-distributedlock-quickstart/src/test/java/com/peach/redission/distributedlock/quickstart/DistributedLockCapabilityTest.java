package com.peach.redission.distributedlock.quickstart;

import com.peach.redission.distributedlock.quickstart.example.InventoryAnnotationLockExample;
import com.peach.redission.distributedlock.quickstart.example.InventoryTemplateLockExample;
import com.peach.redission.distributedlock.quickstart.store.InventoryStockStore;
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
 * 验证分布式锁：注解、Template 与并发竞争。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:55
 */
@SpringBootTest(properties = "quickstart.distributedlock.demo.enabled=false")
@Testcontainers(disabledWithoutDocker = true)
class DistributedLockCapabilityTest {

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
    private InventoryStockStore stockStore;

    @Autowired
    private InventoryAnnotationLockExample annotationLockExample;

    @Autowired
    private InventoryTemplateLockExample templateLockExample;

    @Test
    void annotationShouldProtectDeduct() {
        stockStore.reset("sku-anno-t", 10);
        assertThat(annotationLockExample.deduct("sku-anno-t", 3)).isEqualTo(7);
        assertThat(stockStore.get("sku-anno-t")).isEqualTo(7);
    }

    @Test
    void templateShouldProtectDeduct() {
        stockStore.reset("sku-tpl-t", 5);
        assertThat(templateLockExample.deduct("sku-tpl-t", 2)).isEqualTo(3);
        assertThat(stockStore.get("sku-tpl-t")).isEqualTo(3);
    }

    @Test
    void templateShouldSerializeConcurrentCompete() {
        assertThatCode(() -> templateLockExample.competeOnce("sku-race-t"))
                .doesNotThrowAnyException();
        assertThat(stockStore.get("sku-race-t")).isZero();
    }
}
