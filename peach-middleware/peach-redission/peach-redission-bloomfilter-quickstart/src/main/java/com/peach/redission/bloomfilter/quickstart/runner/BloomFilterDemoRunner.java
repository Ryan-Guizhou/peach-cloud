package com.peach.redission.bloomfilter.quickstart.runner;

import com.peach.redis.bloom.core.BloomStatus;
import com.peach.redission.bloomfilter.quickstart.example.SkuBatchExample;
import com.peach.redission.bloomfilter.quickstart.example.SkuMembershipExample;
import com.peach.redission.bloomfilter.quickstart.example.SkuStatusClearExample;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * 启动后演示 BloomFilterService 的成员判断、批量与状态清理。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:00
 */
@Slf4j
@Indexed
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "quickstart.bloomfilter.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BloomFilterDemoRunner implements ApplicationRunner {

    private final SkuMembershipExample skuMembershipExample;
    private final SkuBatchExample skuBatchExample;
    private final SkuStatusClearExample skuStatusClearExample;

    @Override
    public void run(ApplicationArguments args) {
        runMembershipDemo();
        runBatchDemo();
        runStatusClearDemo();
        log.info("bloomfilter demo finished");
    }

    private void runMembershipDemo() {
        log.info("=== SKU membership init/add/mightContain demo ===");
        skuMembershipExample.resetAndInit();
        skuMembershipExample.add("sku-4001");
        log.info("exists={}, missing={}",
                skuMembershipExample.mightContain("sku-4001"),
                skuMembershipExample.mightContain("sku-missing"));
        skuMembershipExample.clear();
    }

    private void runBatchDemo() {
        log.info("=== SKU batch addAll/mightContainAll demo ===");
        skuBatchExample.resetAndInit();
        List<String> skus = List.of("sku-batch-1", "sku-batch-2", "sku-batch-3");
        skuBatchExample.addAll(skus);
        log.info("allPresent={}, mixed={}",
                skuBatchExample.mightContainAll(skus),
                skuBatchExample.mightContainAll(List.of("sku-batch-1", "sku-absent")));
        skuBatchExample.clear();
    }

    private void runStatusClearDemo() {
        log.info("=== SKU status/segments/clear demo ===");
        skuStatusClearExample.resetInitAndAdd("sku-status-1");
        BloomStatus status = skuStatusClearExample.status();
        log.info("status={}, segments={}",
                status == null ? null : status.toSimpleString(),
                skuStatusClearExample.segments());
        skuStatusClearExample.clear();
        log.info("afterClear mightContain={}", skuStatusClearExample.mightContain("sku-status-1"));
    }
}
