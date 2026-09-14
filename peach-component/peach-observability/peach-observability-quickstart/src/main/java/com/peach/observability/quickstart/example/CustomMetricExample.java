package com.peach.observability.quickstart.example;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

/**
 * 使用 starter 引入的 {@link MeterRegistry} 注册并递增自定义 Counter。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:00
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class CustomMetricExample {

    public static final String METRIC_NAME = "peach.qs.observability.demo.hits";

    private final MeterRegistry meterRegistry;

    /**
     * 递增演示 Counter，并返回递增后的计数值。
     *
     * @return 递增后的 count
     */
    public double incrementHits() {
        Counter counter = meterRegistry.counter(METRIC_NAME, "module", "observability");
        counter.increment();
        double after = counter.count();
        log.info("custom metric increment, name={}, count={}", METRIC_NAME, after);
        return after;
    }
}
