package com.peach.gateway.core.ratelimit.service;

import com.peach.gateway.core.ratelimit.config.GatewayRateLimitProperties;
import com.peach.gateway.core.ratelimit.model.RateLimitAlgorithm;
import com.peach.gateway.core.ratelimit.model.RateLimitRule;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 限流规则注册表。配置刷新采用不可变快照原子替换。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/05
 */
@Component
public class RateLimitRuleRegistry {

    private static final int MAX_BURST_CAPACITY = 1_000_000;

    private static final long MAX_WINDOW_MILLIS = Duration.ofDays(365).toMillis();

    private final GatewayRateLimitProperties properties;
    private final AtomicReference<List<RateLimitRule>> rules = new AtomicReference<>(List.of());

    public RateLimitRuleRegistry(GatewayRateLimitProperties properties) {
        this.properties = properties;
        refresh();
    }

    public List<RateLimitRule> getRules() {
        return rules.get();
    }

    @EventListener(EnvironmentChangeEvent.class)
    public void onEnvironmentChanged(EnvironmentChangeEvent event) {
        refresh();
    }

    /**
     * 校验全部启用规则后一次性替换当前快照。
     */
    public void refresh() {
        List<GatewayRateLimitProperties.RuleProperties> configured = properties.getRules();
        if (configured == null || configured.isEmpty()) {
            rules.set(List.of());
            return;
        }
        Set<String> ruleIds = new LinkedHashSet<>();
        List<RateLimitRule> snapshot = configured.stream()
                .filter(GatewayRateLimitProperties.RuleProperties::isEnabled)
                .map(source -> {
                    RateLimitRule rule = toRule(source);
                    if (!ruleIds.add(rule.id())) {
                        throw new IllegalArgumentException("Duplicate rate limit rule id: " + rule.id());
                    }
                    return rule;
                })
                .sorted(Comparator.comparingInt(RateLimitRule::priority))
                .toList();
        rules.set(snapshot);
    }

    private RateLimitRule toRule(GatewayRateLimitProperties.RuleProperties source) {
        String id = source.getId() == null ? "" : source.getId().trim();
        if (!id.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,63}")
                || source.getScope() == null || source.getAlgorithm() == null) {
            throw new IllegalArgumentException("Rate limit rule id, scope and algorithm are required and valid");
        }

        int cost = source.getCost();
        if (cost <= 0) {
            throw new IllegalArgumentException("Rate limit rule cost must be positive: " + id);
        }

        int limit = source.getLimit();
        int burstCapacity = source.getBurstCapacity();
        long windowMs = requiresWindow(source.getAlgorithm())
                ? resolveWindowMillis(source.getWindow(), id) : 0L;
        double replenishRate = source.getReplenishRate();
        double leakRate = source.getLeakRate();

        switch (source.getAlgorithm()) {
            case FIXED_WINDOW, SLIDING_WINDOW_LOG, SLIDING_WINDOW_COUNTER -> {
                if (limit <= 0 || limit > MAX_BURST_CAPACITY || cost > limit) {
                    throw new IllegalArgumentException("Window limit must be within supported range and >= cost: " + id);
                }
            }
            case TOKEN_BUCKET, GCRA -> validateRateAndCapacity(replenishRate, burstCapacity, cost, id);
            case LEAKY_BUCKET -> validateRateAndCapacity(leakRate, burstCapacity, cost, id);
        }

        List<String> paths = normalizePaths(source.getPaths(), id);
        Set<String> methods = normalizeMethods(source.getMethods());
        Set<String> routeIds = normalizeValues(source.getRouteIds());

        return new RateLimitRule(id, source.isEnabled(), source.getScope(), source.getAlgorithm(),
                source.getPriority(), methods, paths, routeIds, limit, windowMs,
                replenishRate, burstCapacity, leakRate, cost);
    }

    private boolean requiresWindow(RateLimitAlgorithm algorithm) {
        return algorithm == RateLimitAlgorithm.FIXED_WINDOW
                || algorithm == RateLimitAlgorithm.SLIDING_WINDOW_LOG
                || algorithm == RateLimitAlgorithm.SLIDING_WINDOW_COUNTER;
    }

    private long resolveWindowMillis(Duration window, String ruleId) {
        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Rate limit window must be positive: " + ruleId);
        }
        long millis = window.toMillis();
        if (millis <= 0 || millis > MAX_WINDOW_MILLIS) {
            throw new IllegalArgumentException("Rate limit window is outside supported range: " + ruleId);
        }
        return millis;
    }

    private void validateRateAndCapacity(double rate, int capacity, int cost, String ruleId) {
        if (!Double.isFinite(rate) || rate <= 0 || rate > 1_000_000_000D
                || capacity <= 0 || capacity > MAX_BURST_CAPACITY || cost > capacity) {
            throw new IllegalArgumentException("Invalid rate, capacity or cost for rule: " + ruleId);
        }
    }

    private List<String> normalizePaths(List<String> configured, String ruleId) {
        if (configured == null) {
            return List.of();
        }
        List<String> paths = new ArrayList<>();
        for (String path : configured) {
            if (path == null || !path.trim().startsWith("/") || path.contains("?")) {
                throw new IllegalArgumentException("Invalid URI pattern in rate limit rule: " + ruleId);
            }
            paths.add(path.trim());
        }
        return List.copyOf(paths);
    }

    private Set<String> normalizeMethods(List<String> configured) {
        if (configured == null) {
            return Set.of();
        }
        return configured.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private Set<String> normalizeValues(List<String> configured) {
        if (configured == null) {
            return Set.of();
        }
        return configured.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
