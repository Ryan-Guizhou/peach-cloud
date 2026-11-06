package com.peach.common.constant;

/**
 * Sa令牌常量。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
public final class SaTokenConstant {

    private SaTokenConstant() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 包含登录 ID 的 token 签名原文模板。
     */
    public static final String TOKEN_INCLUDE_USER = "{0}-{1}-{2}";

    /**
     * 登录 ID 格式化失败时使用的 token 签名原文模板。
     */
    public static final String TOKEN_NOT_INCLUDE_USER = "{0}-{1}";

    /** 当前 Token Session 中保存的租户 ID。 */
    public static final String TOKEN_SESSION_TENANT_ID = "peach:context:tenantId";

    /** 当前 Token Session 中保存的机构 ID。 */
    public static final String TOKEN_SESSION_ORG_ID = "peach:context:orgId";

    /** 当前 Token Session 中保存的会计期间。 */
    public static final String TOKEN_SESSION_FISCAL = "peach:context:fiscal";

    /** 当前 Token Session 的上下文版本。 */
    public static final String TOKEN_SESSION_CONTEXT_VERSION = "peach:context:version";

    /** 用户 API 资源授权 Redis Set key 前缀。 */
    public static final String USER_API_RESOURCE_CACHE_PREFIX = "peach:security:user:api-resources:";

    /**
     * 构建按用户、租户、机构和会计期间隔离的 API 资源缓存 Key。
     *
     * @param userId 用户 ID
     * @param tenantId 租户 ID
     * @param orgId 机构 ID
     * @param fiscal 会计期间
     * @return API 资源缓存 Key
     */
    public static String userApiResourceCacheKey(String userId, String tenantId, String orgId, String fiscal) {
        return USER_API_RESOURCE_CACHE_PREFIX + userId + ":" + tenantId + ":" + orgId + ":"
                + (fiscal == null ? "" : fiscal);
    }
}
