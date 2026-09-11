package com.peach.redission.distributedlock.quickstart.runner;

import com.peach.redission.distributedlock.quickstart.scenario.InventoryDeductScenarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 启动后演示库存扣减分布式锁。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@Component
@ConditionalOnProperty(prefix = "quickstart.distributedlock.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DistributedLockDemoRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DistributedLockDemoRunner.class);

    private final InventoryDeductScenarioService scenarioService;

    /**
     * @param scenarioService 库存扣减场景服务
     */
    public DistributedLockDemoRunner(InventoryDeductScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    @Override
    public void run(ApplicationArguments args) {
        String skuId = "sku-2001";
        scenarioService.resetStock(skuId, 10);
        int remain = scenarioService.deduct(skuId, 3);
        log.info("distributedlock quickstart finished, skuId={}, remain={}", skuId, remain);
    }
}
