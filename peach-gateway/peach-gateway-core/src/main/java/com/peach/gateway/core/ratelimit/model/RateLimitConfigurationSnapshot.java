package com.peach.gateway.core.ratelimit.model;

import java.util.List;
import java.util.Set;

/**
 * 网关限流配置的不可变运行时快照。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/05
 */
public record RateLimitConfigurationSnapshot(
        List<RateLimitRule> rules,
        Set<String> allowlistIps,
        Set<String> allowlistTenants,
        Set<String> denylistIps,
        Set<String> denylistTenants) {

    public RateLimitConfigurationSnapshot {
        rules = List.copyOf(rules);
        allowlistIps = Set.copyOf(allowlistIps);
        allowlistTenants = Set.copyOf(allowlistTenants);
        denylistIps = Set.copyOf(denylistIps);
        denylistTenants = Set.copyOf(denylistTenants);
    }

    public static RateLimitConfigurationSnapshot empty() {
        return new RateLimitConfigurationSnapshot(List.of(), Set.of(), Set.of(), Set.of(), Set.of());
    }
}
