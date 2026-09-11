package com.peach.redis.multicache.quickstart;

import com.peach.redis.multicache.quickstart.domain.Product;
import com.peach.redis.multicache.quickstart.example.ProductAnnotationCacheExample;
import com.peach.redis.multicache.quickstart.example.ProductManagerCacheExample;
import com.peach.redis.multicache.quickstart.store.InMemoryProductStore;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 MultiCache 的 CacheManager 注入、注解用法，以及 L1/L2 分层命中。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:40
 */
@SpringBootTest(properties = "quickstart.multicache.demo.enabled=false")
@Testcontainers(disabledWithoutDocker = true)
class MulticacheCapabilityTest {

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
    private ProductManagerCacheExample managerExample;

    @Autowired
    private ProductAnnotationCacheExample annotationExample;

    @Autowired
    private InMemoryProductStore productStore;

    @BeforeEach
    void setUp() {
        managerExample.evict("P1001");
        managerExample.evict("P1002");
        annotationExample.evict("P1001");
        productStore.resetLoadCount();
    }

    @Test
    void cacheManagerShouldPutGetAndReloadAfterEvict() {
        Product cached = managerExample.putThenGet("P1001");
        assertThat(cached).isNotNull();
        assertThat(cached.getId()).isEqualTo("P1001");

        productStore.resetLoadCount();
        Product first = managerExample.getOrLoad("P1002");
        Product second = managerExample.getOrLoad("P1002");
        assertThat(first).isNotNull();
        assertThat(second.getName()).isEqualTo(first.getName());
        assertThat(productStore.loadCount()).isEqualTo(1);

        managerExample.evict("P1002");
        productStore.resetLoadCount();
        Product afterEvict = managerExample.getOrLoad("P1002");
        assertThat(afterEvict).isNotNull();
        assertThat(productStore.loadCount()).isEqualTo(1);
    }

    @Test
    void shouldHitRedisAndRefillLocalAfterClearLocal() {
        Product restored = managerExample.proveLocalMissHitsRedis("P1001");
        assertThat(restored).isNotNull();
        assertThat(restored.getId()).isEqualTo("P1001");
        assertThat(productStore.loadCount()).isZero();
    }

    @Test
    void annotationShouldCacheAndReloadAfterEvict() {
        Product first = annotationExample.getById("P1001");
        Product second = annotationExample.getById("P1001");
        assertThat(first).isNotNull();
        assertThat(second.getName()).isEqualTo(first.getName());
        assertThat(productStore.loadCount()).isEqualTo(1);

        annotationExample.evict("P1001");
        productStore.resetLoadCount();
        Product afterEvict = annotationExample.getById("P1001");
        assertThat(afterEvict).isNotNull();
        assertThat(productStore.loadCount()).isEqualTo(1);
    }
}
