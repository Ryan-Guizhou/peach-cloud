package com.peach.redission.distributedlock.quickstart.scenario;

import com.peach.redission.distrbutedlock.support.DistributedLockTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 库存扣减场景：演示 DistributedLockTemplate.call。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@Service
public class InventoryDeductScenarioService {

    private static final String BUSINESS_NAME = "inventory-deduct";

    private final DistributedLockTemplate distributedLockTemplate;
    private final ConcurrentMap<String, Integer> stockStore = new ConcurrentHashMap<>();

    /**
     * @param distributedLockTemplate 编程式分布式锁模板
     */
    public InventoryDeductScenarioService(DistributedLockTemplate distributedLockTemplate) {
        this.distributedLockTemplate = distributedLockTemplate;
    }

    /**
     * 重置演示库存。
     *
     * @param skuId 商品标识
     * @param stock 初始库存
     */
    public void resetStock(String skuId, int stock) {
        stockStore.put(skuId, stock);
    }

    /**
     * 在分布式锁保护下扣减库存。
     *
     * @param skuId  商品标识
     * @param amount 扣减数量
     * @return 扣减后的剩余库存
     */
    public int deduct(String skuId, int amount) {
        return distributedLockTemplate.call(BUSINESS_NAME, skuId, () -> {
            int current = stockStore.getOrDefault(skuId, 0);
            if (current < amount) {
                throw new IllegalStateException("Insufficient stock for sku=" + skuId);
            }
            int remain = current - amount;
            stockStore.put(skuId, remain);
            return remain;
        });
    }

    /**
     * 查询当前库存。
     *
     * @param skuId 商品标识
     * @return 当前库存
     */
    public int getStock(String skuId) {
        return stockStore.getOrDefault(skuId, 0);
    }
}
