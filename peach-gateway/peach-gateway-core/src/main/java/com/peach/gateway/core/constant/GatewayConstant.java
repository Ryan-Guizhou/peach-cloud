package com.peach.gateway.core.constant;

import com.peach.common.constant.SaTokenConstant;

/**
 * 网关安全常量。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/11 14:45
 */
public final class GatewayConstant {

    private GatewayConstant() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 包含登录 ID 的 token 签名原文模板。
     *
     * @deprecated 请使用 {@link SaTokenConstant#TOKEN_INCLUDE_USER}
     */
    @Deprecated
    public static final String TOKEN_INCLUDE_USER = SaTokenConstant.TOKEN_INCLUDE_USER;

    /**
     * 登录 ID 格式化失败时使用的 token 签名原文模板。
     *
     * @deprecated 请使用 {@link SaTokenConstant#TOKEN_NOT_INCLUDE_USER}
     */
    @Deprecated
    public static final String TOKEN_NOT_INCLUDE_USER = SaTokenConstant.TOKEN_NOT_INCLUDE_USER;

    /**
     * 网关可信请求 ID 请求头。
     */
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    /**
     * 动态客户端地址黑名单 Redis Set key。
     */
    public static final String RISK_CONTROL_BLOCKED_IP_KEY = "peach:gateway:risk-control:blocked-ip:";

    /**
     * 动态客户端地址白名单 Redis Set key。
     */
    public static final String RISK_CONTROL_WHITELIST_IP_KEY = "peach:gateway:risk-control:whitelist-ip";

    /**
     * 用户 API 资源授权 Redis Set key 前缀。
     */
    public static final String USER_API_RESOURCE_CACHE_PREFIX = "peach:security:user:api-resources:";

}
