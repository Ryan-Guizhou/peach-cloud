package com.peach.redis.multicache.quickstart.runner;

import com.peach.redis.multicache.quickstart.domain.Product;
import com.peach.redis.multicache.quickstart.example.ProductAnnotationCacheExample;
import com.peach.redis.multicache.quickstart.example.ProductManagerCacheExample;
import com.peach.redis.multicache.quickstart.store.InMemoryProductStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

/**
 * 启动后演示 CacheManager 编程注入与注解两种多级缓存用法。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:40
 */
@Slf4j
@Indexed
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "quickstart.multicache.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MulticacheDemoRunner implements ApplicationRunner {

    private final ProductManagerCacheExample managerExample;
    private final ProductAnnotationCacheExample annotationExample;
    private final InMemoryProductStore productStore;

    @Override
    public void run(ApplicationArguments args) {
        runManagerDemo();
        runAnnotationDemo();
        log.info("multicache demo finished");
    }

    private void runManagerDemo() {
        productStore.resetLoadCount();
        log.info("=== CacheManager API demo ===");

        Product warmed = managerExample.putThenGet("P1001");
        log.info("cacheManager putThenGet result={}", warmed);

        productStore.resetLoadCount();
        Product first = managerExample.getOrLoad("P1002");
        Product second = managerExample.getOrLoad("P1002");
        log.info("cacheManager getOrLoad first={}, second={}, storeLoads={}", first, second, productStore.loadCount());

        managerExample.evict("P1002");
        productStore.resetLoadCount();
        Product afterEvict = managerExample.getOrLoad("P1002");
        log.info("cacheManager afterEvict result={}, storeLoads={}", afterEvict, productStore.loadCount());

        Product layered = managerExample.proveLocalMissHitsRedis("P1001");
        log.info("cacheManager layered proof result={}", layered);
    }

    private void runAnnotationDemo() {
        productStore.resetLoadCount();
        log.info("=== Annotation API demo ===");

        Product first = annotationExample.getById("P1001");
        Product second = annotationExample.getById("P1001");
        log.info("annotation getById first={}, second={}, storeLoads={}", first, second, productStore.loadCount());

        annotationExample.evict("P1001");
        productStore.resetLoadCount();
        Product afterEvict = annotationExample.getById("P1001");
        log.info("annotation afterEvict result={}, storeLoads={}", afterEvict, productStore.loadCount());
    }
}
