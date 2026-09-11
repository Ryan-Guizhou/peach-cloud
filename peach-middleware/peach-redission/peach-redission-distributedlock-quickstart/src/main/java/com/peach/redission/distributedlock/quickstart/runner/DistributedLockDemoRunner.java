package com.peach.redission.distributedlock.quickstart.runner;

import com.peach.redission.distributedlock.quickstart.example.InventoryAnnotationLockExample;
import com.peach.redission.distributedlock.quickstart.example.InventoryTemplateLockExample;
import com.peach.redission.distributedlock.quickstart.store.InventoryStockStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

/**
 * 启动后演示注解锁、Template 扣库存与并发竞争。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:55
 */
@Slf4j
@Indexed
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "quickstart.distributedlock.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DistributedLockDemoRunner implements ApplicationRunner {

    private final InventoryStockStore stockStore;
    private final InventoryAnnotationLockExample annotationLockExample;
    private final InventoryTemplateLockExample templateLockExample;

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== Annotation @DistrbutedLock demo ===");
        stockStore.reset("sku-anno-1", 10);
        int remainAnno = annotationLockExample.deduct("sku-anno-1", 3);
        log.info("annotation remain={}", remainAnno);

        log.info("=== Template deduct demo ===");
        stockStore.reset("sku-tpl-1", 10);
        int remainTpl = templateLockExample.deduct("sku-tpl-1", 2);
        log.info("template remain={}", remainTpl);

        log.info("=== Concurrent compete demo ===");
        templateLockExample.competeOnce("sku-race-1");
        log.info("distributedlock demo finished");
    }
}
