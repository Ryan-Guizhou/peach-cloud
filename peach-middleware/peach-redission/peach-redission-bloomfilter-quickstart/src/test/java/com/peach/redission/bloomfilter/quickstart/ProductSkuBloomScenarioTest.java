package com.peach.redission.bloomfilter.quickstart;

import com.peach.redission.bloomfilter.quickstart.scenario.ProductSkuBloomScenarioService;
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
 * 基于 Testcontainers Redis 验证布隆过滤器 add/mightContain。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@SpringBootTest(properties = "quickstart.bloomfilter.demo.enabled=false")
@Testcontainers(disabledWithoutDocker = true)
class ProductSkuBloomScenarioTest {

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
        registry.add("peach.redis.bloom.enabled", () -> "true");
    }

    @Autowired
    private ProductSkuBloomScenarioService scenarioService;

    @Test
    void shouldRegisterSkuAndMightContain() {
        scenarioService.clear();
        String skuId = "it-sku-bloom";
        scenarioService.registerSku(skuId);
        assertThat(scenarioService.mightContain(skuId)).isTrue();
        assertThat(scenarioService.mightContain("missing-sku")).isFalse();
    }
}
