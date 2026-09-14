package com.peach.redis.multicache.quickstart.store;

import com.peach.redis.multicache.quickstart.domain.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 模拟商品库，通过 loadCount 观察缓存命中是否真正回源。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:40
 */
@Slf4j
@Indexed
@Component
public class InMemoryProductStore {

    private final Map<String, Product> products = new ConcurrentHashMap<>();
    private final AtomicInteger loadCount = new AtomicInteger();

    public InMemoryProductStore() {
        products.put("P1001", new Product("P1001", "Peach Juice", new BigDecimal("19.90")));
        products.put("P1002", new Product("P1002", "Peach Tea", new BigDecimal("12.50")));
    }

    /**
     * 按 id 查询商品；每次调用视为一次回源。
     */
    public Product findById(String id) {
        int count = loadCount.incrementAndGet();
        log.info("load product from store, id={}, loadCount={}", id, count);
        Product product = products.get(id);
        if (product == null) {
            return null;
        }
        return new Product(product.getId(), product.getName(), product.getPrice());
    }

    public void save(Product product) {
        products.put(product.getId(), new Product(product.getId(), product.getName(), product.getPrice()));
    }

    public int loadCount() {
        return loadCount.get();
    }

    public void resetLoadCount() {
        loadCount.set(0);
    }
}
