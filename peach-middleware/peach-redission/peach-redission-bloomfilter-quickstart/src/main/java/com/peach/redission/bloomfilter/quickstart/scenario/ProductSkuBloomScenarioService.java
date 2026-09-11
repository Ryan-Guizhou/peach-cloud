package com.peach.redission.bloomfilter.quickstart.scenario;

import com.peach.redis.bloom.core.BloomFilterService;
import org.springframework.stereotype.Service;

/**
 * 商品 SKU 布隆过滤场景：演示 initNamespace/add/mightContain。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@Service
public class ProductSkuBloomScenarioService {

    public static final String NAMESPACE = "product-sku";

    private final BloomFilterService bloomFilterService;

    /**
     * @param bloomFilterService 布隆过滤器服务
     */
    public ProductSkuBloomScenarioService(BloomFilterService bloomFilterService) {
        this.bloomFilterService = bloomFilterService;
    }

    /**
     * 初始化命名空间并写入 SKU。
     *
     * @param skuId 商品标识
     */
    public void registerSku(String skuId) {
        bloomFilterService.initNamespace(NAMESPACE, 1000L, 0.01d);
        bloomFilterService.add(NAMESPACE, skuId);
    }

    /**
     * 判断 SKU 是否可能存在。
     *
     * @param skuId 商品标识
     * @return true 表示可能存在
     */
    public boolean mightContain(String skuId) {
        return bloomFilterService.mightContain(NAMESPACE, skuId);
    }

    /**
     * 清理命名空间。
     */
    public void clear() {
        bloomFilterService.clear(NAMESPACE);
    }
}
