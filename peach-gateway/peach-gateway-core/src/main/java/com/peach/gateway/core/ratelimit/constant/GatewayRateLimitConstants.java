package com.peach.gateway.core.ratelimit.constant;

import com.peach.common.constant.SaTokenConstant;

/**
 * 网关限流常量。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/05
 */
public final class GatewayRateLimitConstants {

    public static final String REDIS_KEY_PREFIX = "peach:gateway:rate-limit:v1:";

    public static final String TOKEN_SESSION_TENANT_ID = SaTokenConstant.TOKEN_SESSION_TENANT_ID;

    public static final String TOKEN_SESSION_ORG_ID = SaTokenConstant.TOKEN_SESSION_ORG_ID;

    public static final String TOKEN_SESSION_FISCAL = SaTokenConstant.TOKEN_SESSION_FISCAL;

    public static final String TENANT_CONTEXT_ATTRIBUTE = "peach.gateway.rate-limit.tenant-id";

    public static final String RATE_LIMIT_LIMIT_HEADER = "RateLimit-Limit";

    public static final String RATE_LIMIT_REMAINING_HEADER = "RateLimit-Remaining";

    public static final String RATE_LIMIT_RESET_HEADER = "RateLimit-Reset";

    public static final String RETRY_AFTER_HEADER = "Retry-After";

    private GatewayRateLimitConstants() {
        throw new IllegalStateException("Utility class");
    }
}
