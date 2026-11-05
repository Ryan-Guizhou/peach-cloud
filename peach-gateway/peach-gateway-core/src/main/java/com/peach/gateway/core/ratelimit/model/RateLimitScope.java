package com.peach.gateway.core.ratelimit.model;

/**
 * 网关限流维度。
 */
public enum RateLimitScope {

    GLOBAL,
    IP,
    URI,
    IP_URI,
    TENANT,
    TENANT_URI;

    public boolean isTenantScope() {
        return this == TENANT || this == TENANT_URI;
    }

    public boolean isIpScope() {
        return this == IP || this == IP_URI;
    }

    public boolean isUriScope() {
        return this == URI || this == IP_URI || this == TENANT_URI;
    }
}
