package com.peach.gateway.core.ratelimit.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 网关限流指标测试。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/06
 */
class RateLimitMetricsTest {

    @Test
    void shouldExposeScriptReadinessAndReloadOutcomesWithLowCardinalityTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("meterRegistry", registry);
        RateLimitMetrics metrics = new RateLimitMetrics(beanFactory.getBeanProvider(
                io.micrometer.core.instrument.MeterRegistry.class));

        metrics.updateScriptReady(true);
        metrics.recordScriptReload(true);
        metrics.recordScriptReload(false);

        assertThat(registry.get("peach.gateway.rate.limit.scripts.ready").gauge().value()).isEqualTo(1D);
        assertThat(registry.get("peach.gateway.rate.limit.scripts.reloads")
                .tag("outcome", "success").counter().count()).isEqualTo(1D);
        assertThat(registry.get("peach.gateway.rate.limit.scripts.reloads")
                .tag("outcome", "failure").counter().count()).isEqualTo(1D);
    }
}
