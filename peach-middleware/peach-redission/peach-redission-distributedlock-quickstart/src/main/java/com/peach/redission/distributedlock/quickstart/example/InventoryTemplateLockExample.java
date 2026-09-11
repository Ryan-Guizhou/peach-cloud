package com.peach.redission.distributedlock.quickstart.example;

import com.peach.redission.distrbutedlock.locker.LockType;
import com.peach.redission.distrbutedlock.support.DistributedLockTemplate;
import com.peach.redission.distributedlock.quickstart.store.InventoryStockStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Template 方式：注入 {@link DistributedLockTemplate} 做编程式加锁与并发竞争。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:55
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class InventoryTemplateLockExample {

    private final DistributedLockTemplate distributedLockTemplate;
    private final InventoryStockStore stockStore;

    /**
     * 编程式扣库存。
     */
    public int deduct(String sku, int amount) {
        return distributedLockTemplate.call("inventory-template", sku, () -> {
            int remain = stockStore.deduct(sku, amount);
            log.info("template deduct ok, sku={}, amount={}, remain={}", sku, amount, remain);
            return remain;
        });
    }

    /**
     * 双线程短 wait 竞争同一把锁：期望 1 成功 1 失败，库存扣到 0。
     */
    public void competeOnce(String sku) {
        stockStore.reset(sku, 1);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();

        Runnable worker = () -> {
            ready.countDown();
            try {
                start.await(5, TimeUnit.SECONDS);
                distributedLockTemplate.call(
                        "inventory-race",
                        new String[]{sku},
                        LockType.REENTRANT,
                        1L,
                        TimeUnit.SECONDS,
                        () -> {
                            try {
                                Thread.sleep(1500L);
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                                throw new IllegalStateException("interrupted in critical section", ex);
                            }
                            int remain = stockStore.deduct(sku, 1);
                            success.incrementAndGet();
                            return remain;
                        });
            } catch (Exception ex) {
                failed.incrementAndGet();
                log.info("compete worker failed: {}", ex.getMessage());
            }
        };

        Thread t1 = new Thread(worker, "lock-race-1");
        Thread t2 = new Thread(worker, "lock-race-2");
        t1.start();
        t2.start();
        try {
            if (!ready.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("workers not ready");
            }
            start.countDown();
            t1.join(10_000L);
            t2.join(10_000L);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("compete interrupted", ex);
        }

        log.info("compete result success={}, failed={}, remain={}", success.get(), failed.get(), stockStore.get(sku));
        if (success.get() != 1 || failed.get() != 1 || stockStore.get(sku) != 0) {
            throw new IllegalStateException("compete self-check failed");
        }
    }
}
