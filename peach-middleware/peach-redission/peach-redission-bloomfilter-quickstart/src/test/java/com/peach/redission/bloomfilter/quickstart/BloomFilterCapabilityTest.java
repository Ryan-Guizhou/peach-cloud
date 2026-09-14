package com.peach.redission.bloomfilter.quickstart;

import com.peach.redis.bloom.core.BloomStatus;
import com.peach.redission.bloomfilter.quickstart.example.SkuBatchExample;
import com.peach.redission.bloomfilter.quickstart.example.SkuMembershipExample;
import com.peach.redission.bloomfilter.quickstart.example.SkuStatusClearExample;
import org.junit.jupiter.api.BeforeEach;
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
 * 验证 BloomFilterService 对外能力：成员判断、批量与状态清理。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:00
 */
@SpringBootTest(properties = "quickstart.bloomfilter.demo.enabled=false")
@Testcontainers(disabledWithoutDocker = true)
class BloomFilterCapabilityTest {

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
    private SkuMembershipExample skuMembershipExample;

    @Autowired
    private SkuBatchExample skuBatchExample;

    @Autowired
    private SkuStatusClearExample skuStatusClearExample;

    @BeforeEach
    void clean() {
        skuMembershipExample.clear();
        skuBatchExample.clear();
        skuStatusClearExample.clear();
    }

    @Test
    void membershipShouldDetectPresentAndAbsentSku() {
        skuMembershipExample.resetAndInit();
        skuMembershipExample.add("sku-4001");

        assertThat(skuMembershipExample.mightContain("sku-4001")).isTrue();
        assertThat(skuMembershipExample.mightContain("sku-missing")).isFalse();
    }

    @Test
    void batchShouldRequireEveryValueToBePresent() {
        skuBatchExample.resetAndInit();
        List<String> skus = List.of("sku-batch-1", "sku-batch-2", "sku-batch-3");
        skuBatchExample.addAll(skus);

        assertThat(skuBatchExample.mightContainAll(skus)).isTrue();
        assertThat(skuBatchExample.mightContainAll(List.of("sku-batch-1", "sku-absent"))).isFalse();
    }

    @Test
    void statusShouldReportSegmentsAndClearShouldRemoveValues() {
        skuStatusClearExample.resetInitAndAdd("sku-status-1");

        BloomStatus status = skuStatusClearExample.status();
        assertThat(status).isNotNull();
        assertThat(status.segments).isGreaterThanOrEqualTo(1);
        assertThat(skuStatusClearExample.segments()).isGreaterThanOrEqualTo(1);
        assertThat(skuStatusClearExample.mightContain("sku-status-1")).isTrue();

        skuStatusClearExample.clear();
        assertThat(skuStatusClearExample.mightContain("sku-status-1")).isFalse();
        assertThat(skuStatusClearExample.segments()).isZero();
    }
}
