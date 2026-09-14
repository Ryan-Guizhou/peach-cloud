package com.peach.redission.bloomfilter.quickstart.example;

import com.peach.redis.bloom.core.BloomFilterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * 商品 SKU 批量写入与批量判断。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:00
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class SkuBatchExample {

    public static final String NAMESPACE = "qs-sku-batch";

    private final BloomFilterService bloomFilterService;

    /**
     * 清空并初始化命名空间。
     */
    public void resetAndInit() {
        bloomFilterService.clear(NAMESPACE);
        bloomFilterService.initNamespace(NAMESPACE, 1000L, 0.01d);
        log.info("sku batch init, namespace={}", NAMESPACE);
    }

    /**
     * 批量写入 SKU。
     */
    public void addAll(Collection<String> skus) {
        bloomFilterService.addAll(NAMESPACE, skus);
        log.info("sku batch addAll, count={}", skus == null ? 0 : skus.size());
    }

    /**
     * 批量判断：全部可能存在才返回 true。
     */
    public boolean mightContainAll(Collection<String> skus) {
        boolean allPresent = bloomFilterService.mightContainAll(NAMESPACE, skus);
        log.info("sku batch mightContainAll, count={}, allPresent={}", skus == null ? 0 : skus.size(), allPresent);
        return allPresent;
    }

    /**
     * 清理本样例命名空间，供测试隔离。
     */
    public void clear() {
        bloomFilterService.clear(NAMESPACE);
    }
}
