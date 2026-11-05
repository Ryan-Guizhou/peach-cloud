package com.peach.gateway.core.ratelimit.model;

import java.util.List;
import java.util.Set;

/**
 * 校验完成后的不可变限流规则。
  *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/05
*/
public record RateLimitRule(
        String id,
        boolean enabled,
        RateLimitScope scope,
        RateLimitAlgorithm algorithm,
        int priority,
        Set<String> methods,
        List<String> paths,
        Set<String> routeIds,
        int limit,
        long windowMs,
        double replenishRate,
        int burstCapacity,
        double leakRate,
        int cost) {
}
