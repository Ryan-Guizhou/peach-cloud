package com.peach.redission.bloomfilter.quickstart.example;

import com.peach.redis.bloom.core.BloomFilterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

/**
 * 商品 SKU 成员判断：初始化命名空间后写入单值，再判断存在 / 不存在。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:00
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class SkuMembershipExample {

    public static final String NAMESPACE = "qs-sku-member";

    private final BloomFilterService bloomFilterService;

    /**
     * 清空并初始化命名空间（仅在段不存在时创建首段）。
     */
    public void resetAndInit() {
        bloomFilterService.clear(NAMESPACE);
        bloomFilterService.initNamespace(NAMESPACE, 1000L, 0.01d);
        log.info("sku membership init, namespace={}", NAMESPACE);
    }

    /**
     * 写入单个 SKU。
     *
     * @return true 表示该值此前可能不存在（近似语义）
     */
    public boolean add(String sku) {
        boolean added = bloomFilterService.add(NAMESPACE, sku);
        log.info("sku membership add, sku={}, added={}", sku, added);
        return added;
    }

    /**
     * 判断 SKU 是否可能存在；命中仍需回查权威数据源。
     */
    public boolean mightContain(String sku) {
        boolean present = bloomFilterService.mightContain(NAMESPACE, sku);
        log.info("sku membership mightContain, sku={}, present={}", sku, present);
        return present;
    }

    /**
     * 清理本样例命名空间，供测试隔离。
     */
    public void clear() {
        bloomFilterService.clear(NAMESPACE);
    }
}
