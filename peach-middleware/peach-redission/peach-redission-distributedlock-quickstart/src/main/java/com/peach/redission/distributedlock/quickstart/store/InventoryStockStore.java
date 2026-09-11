package com.peach.redission.distributedlock.quickstart.store;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 内存库存，用于演示分布式锁临界区。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:55
 */
@Indexed
@Component
public class InventoryStockStore {

    private final ConcurrentHashMap<String, AtomicInteger> stocks = new ConcurrentHashMap<>();

    public void reset(String sku, int quantity) {
        stocks.put(sku, new AtomicInteger(quantity));
    }

    public int get(String sku) {
        AtomicInteger stock = stocks.get(sku);
        return stock == null ? 0 : stock.get();
    }

    public int deduct(String sku, int amount) {
        AtomicInteger stock = stocks.computeIfAbsent(sku, ignored -> new AtomicInteger(0));
        int current = stock.get();
        if (current < amount) {
            throw new IllegalStateException("insufficient stock, sku=" + sku);
        }
        return stock.addAndGet(-amount);
    }
}
