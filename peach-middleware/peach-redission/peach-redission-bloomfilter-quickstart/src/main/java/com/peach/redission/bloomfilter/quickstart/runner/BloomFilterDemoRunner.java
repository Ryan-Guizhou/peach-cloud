package com.peach.redission.bloomfilter.quickstart.runner;

import com.peach.redission.bloomfilter.quickstart.scenario.ProductSkuBloomScenarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 启动后演示布隆过滤器写入与查询。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@Component
@ConditionalOnProperty(prefix = "quickstart.bloomfilter.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BloomFilterDemoRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BloomFilterDemoRunner.class);

    private final ProductSkuBloomScenarioService scenarioService;

    /**
     * @param scenarioService 布隆场景服务
     */
    public BloomFilterDemoRunner(ProductSkuBloomScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    @Override
    public void run(ApplicationArguments args) {
        String skuId = "sku-4001";
        scenarioService.clear();
        scenarioService.registerSku(skuId);
        boolean exists = scenarioService.mightContain(skuId);
        log.info("bloomfilter quickstart finished, skuId={}, mightContain={}", skuId, exists);
    }
}
