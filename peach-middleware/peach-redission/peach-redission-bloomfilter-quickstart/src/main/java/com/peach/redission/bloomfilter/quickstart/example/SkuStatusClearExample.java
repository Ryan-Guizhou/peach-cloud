package com.peach.redission.bloomfilter.quickstart.example;

import com.peach.redis.bloom.core.BloomFilterService;
import com.peach.redis.bloom.core.BloomStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

/**
 * 查询命名空间状态、段数，并清理全部段。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:00
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class SkuStatusClearExample {

    public static final String NAMESPACE = "qs-sku-status";

    private final BloomFilterService bloomFilterService;

    /**
     * 清空并初始化命名空间后写入一个样例 SKU，便于观察 status。
     */
    public void resetInitAndAdd(String sku) {
        bloomFilterService.clear(NAMESPACE);
        bloomFilterService.initNamespace(NAMESPACE, 1000L, 0.01d);
        bloomFilterService.add(NAMESPACE, sku);
        log.info("sku status init, namespace={}, sku={}", NAMESPACE, sku);
    }

    /**
     * 查询命名空间近似计数、容量、段数与综合 FPP。
     */
    public BloomStatus status() {
        BloomStatus status = bloomFilterService.status(NAMESPACE);
        log.info("sku status snapshot, summary={}", status == null ? null : status.toSimpleString());
        return status;
    }

    /**
     * 当前命名空间段数量。
     */
    public int segments() {
        int segments = bloomFilterService.segments(NAMESPACE);
        log.info("sku status segments={}", segments);
        return segments;
    }

    /**
     * 判断 SKU 是否可能仍存在（用于验证 clear 之后）。
     */
    public boolean mightContain(String sku) {
        return bloomFilterService.mightContain(NAMESPACE, sku);
    }

    /**
     * 清理命名空间下的所有段与计数。
     */
    public void clear() {
        bloomFilterService.clear(NAMESPACE);
        log.info("sku status cleared, namespace={}", NAMESPACE);
    }
}
