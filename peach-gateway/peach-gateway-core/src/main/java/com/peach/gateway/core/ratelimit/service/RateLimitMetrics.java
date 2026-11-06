package com.peach.gateway.core.ratelimit.service;

import com.peach.gateway.core.ratelimit.model.RateLimitRule;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 限流指标记录器。指标标签不包含 IP、租户、用户、URI 或规则 ID，避免高基数。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/05
 */
@Component
@RequiredArgsConstructor
public class RateLimitMetrics {

    private final ObjectProvider<MeterRegistry> registryProvider;

    private final AtomicInteger scriptsReady = new AtomicInteger();

    public void updateScriptReady(boolean ready) {
        scriptsReady.set(ready ? 1 : 0);
        MeterRegistry registry = registryProvider.getIfAvailable();
        if (registry == null || registry.find("peach.gateway.rate.limit.scripts.ready").gauge() != null) {
            return;
        }
        Gauge.builder("peach.gateway.rate.limit.scripts.ready", scriptsReady, AtomicInteger::get)
                .description("1 when every gateway rate limit Lua script is loaded")
                .register(registry);
    }

    public void record(RateLimitRule rule, String outcome, long durationNanos) {
        MeterRegistry registry = registryProvider.getIfAvailable();
        if (registry == null) {
            return;
        }
        String scope = rule.scope().name().toLowerCase(Locale.ROOT);
        String algorithm = rule.algorithm().getConfigKey();
        Counter.builder("peach.gateway.rate.limit.requests")
                .description("Gateway rate limit decisions")
                .tag("scope", scope)
                .tag("algorithm", algorithm)
                .tag("outcome", outcome)
                .register(registry)
                .increment();
        Timer.builder("peach.gateway.rate.limit.execution")
                .description("Gateway rate limit Redis execution latency")
                .tag("scope", scope)
                .tag("algorithm", algorithm)
                .register(registry)
                .record(Math.max(0L, durationNanos), TimeUnit.NANOSECONDS);
    }

    public void recordPolicy(String outcome) {
        MeterRegistry registry = registryProvider.getIfAvailable();
        if (registry == null) {
            return;
        }
        Counter.builder("peach.gateway.rate.limit.requests")
                .description("Gateway rate limit decisions")
                .tag("scope", "policy")
                .tag("algorithm", "none")
                .tag("outcome", outcome)
                .register(registry)
                .increment();
    }
}
