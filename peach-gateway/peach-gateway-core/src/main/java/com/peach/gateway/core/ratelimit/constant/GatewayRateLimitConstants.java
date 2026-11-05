package com.peach.gateway.core.ratelimit.constant;

/**
 * 网关限流常量。
 */
public final class GatewayRateLimitConstants {

    public static final String REDIS_KEY_PREFIX = "peach:gateway:rate-limit:v1:";

    public static final String TOKEN_SESSION_TENANT_ID = "tenantId";

    public static final String TOKEN_SESSION_ORG_ID = "orgId";

    public static final String TOKEN_SESSION_FISCAL = "fiscal";

    public static final String TENANT_CONTEXT_ATTRIBUTE = "peach.gateway.rate-limit.tenant-id";

    private GatewayRateLimitConstants() {
        throw new IllegalStateException("Utility class");
    }
}
