package com.peach.gateway.core.ratelimit.model;

import com.peach.gateway.core.ratelimit.config.GatewayRateLimitProperties;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 网关限流配置的不可变运行时快照。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/05
 */
public record RateLimitConfigurationSnapshot(
        long generation,
        boolean enabled,
        GatewayRateLimitProperties.FailureMode failureMode,
        boolean requireTenantContext,
        int maxMatchedRulesPerRequest,
        List<String> trustedProxies,
        List<RateLimitRule> rules,
        Set<String> allowlistIps,
        Set<String> allowlistTenants,
        Set<String> denylistIps,
        Set<String> denylistTenants) {

    public RateLimitConfigurationSnapshot {
        if (generation < 0) {
            throw new IllegalArgumentException("Rate limit configuration generation must not be negative");
        }
        Objects.requireNonNull(failureMode, "failureMode");
        if (maxMatchedRulesPerRequest < 1) {
            throw new IllegalArgumentException("maxMatchedRulesPerRequest must be positive");
        }
        trustedProxies = List.copyOf(trustedProxies);
        rules = List.copyOf(rules);
        allowlistIps = Set.copyOf(allowlistIps);
        allowlistTenants = Set.copyOf(allowlistTenants);
        denylistIps = Set.copyOf(denylistIps);
        denylistTenants = Set.copyOf(denylistTenants);
    }

    public static RateLimitConfigurationSnapshot empty() {
        return new RateLimitConfigurationSnapshot(
                0L,
                false,
                GatewayRateLimitProperties.FailureMode.FAIL_CLOSED,
                true,
                32,
                List.of(),
                List.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of());
    }
}
