package com.peach.redission.distributedlock.quickstart.example;

import com.peach.redission.distrbutedlock.annoation.DistrbutedLock;
import com.peach.redission.distributedlock.quickstart.store.InventoryStockStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

/**
 * 注解方式：{@code @DistrbutedLock} 保护扣库存临界区。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:55
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class InventoryAnnotationLockExample {

    private final InventoryStockStore stockStore;

    @DistrbutedLock(name = "inventory-annotation", keys = {"#p0"}, waitTime = 5)
    public int deduct(String sku, int amount) {
        int remain = stockStore.deduct(sku, amount);
        log.info("annotation deduct ok, sku={}, amount={}, remain={}", sku, amount, remain);
        return remain;
    }
}
