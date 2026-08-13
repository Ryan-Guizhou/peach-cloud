package com.peach.gateway.core.constant;

/**
 * 网关安全常量。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/11 14:45
 */
public interface GatewayConstant {

    /**
     * 包含登录 ID 的 token 签名原文模板。
     */
    String TOKEN_INCLUDE_USER = "{0}-{1}-{2}";

    /**
     * 登录 ID 格式化失败时使用的 token 签名原文模板。
     */
    String TOKEN_NOT_INCLUDE_USER = "{0}-{1}";

    /**
     * 网关可信请求 ID 请求头。
     */
    String REQUEST_ID_HEADER = "X-Request-Id";

    /**
     * 动态客户端地址黑名单 Redis Set key。
     */
    String RISK_CONTROL_BLOCKED_IP_KEY = "peach:gateway:risk-control:blocked-ip:";

    /**
     * 动态客户端地址白名单 Redis Set key。
     */
    String RISK_CONTROL_WHITELIST_IP_KEY = "peach:gateway:risk-control:whitelist-ip";

    /**
     * 用户 API 资源授权 Redis Set key 前缀。
     */
    String USER_API_RESOURCE_CACHE_PREFIX = "peach:security:user:api-resources:";

}
