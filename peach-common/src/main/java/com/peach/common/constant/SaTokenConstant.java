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

    /** 当前 Token Session 的不可拆分 SaaS 身份快照。 */
    public static final String TOKEN_SESSION_SAAS_CONTEXT = "peach:context:saas";

    /** Saas 用户物理主键，服务端确认身份后写入。 */
    public static final String TOKEN_SESSION_USER_REF_ID = "peach:context:userRefId";

    /** 当前 Saas 租户物理主键。 */
    public static final String TOKEN_SESSION_TENANT_REF_ID = "peach:context:tenantRefId";

    /** 当前 Saas 成员物理主键。 */
    public static final String TOKEN_SESSION_MEMBER_REF_ID = "peach:context:memberRefId";

    /** 当前 Saas 应用物理主键。 */
    public static final String TOKEN_SESSION_APP_REF_ID = "peach:context:appRefId";

    /** 成员身份的乐观版本；成员变更后旧凭证校验失败。 */
    public static final String TOKEN_SESSION_MEMBER_VERSION = "peach:context:memberVersion";

    /** 当前 Token Session 中保存的租户 ID。 */
    public static final String TOKEN_SESSION_TENANT_ID = "peach:context:tenantId";

    /** 当前 Token Session 中保存的机构 ID。 */
    public static final String TOKEN_SESSION_ORG_ID = "peach:context:orgId";

    /** 当前 Token Session 的上下文版本。 */
    public static final String TOKEN_SESSION_CONTEXT_VERSION = "peach:context:version";

    /** SaaS 成员独立应用和权限版本的 API 授权 Redis Set key 前缀。 */
    public static final String MEMBER_API_RESOURCE_CACHE_PREFIX = "peach:security:member:api-resources:";

    /**
     * 构造与当前成员授权版本完全绑定的 API 资源缓存键。
     *
     * <p>仅接受来自服务端可信 Token Session 且经版本重新验证的参数，
     * 旧的按用户/机构/FISCAL 组成的缓存绝不参与 SaaS 授权。</p>
     *
     * @param tenantRefId 当前租户物理主键
     * @param memberRefId 当前成员物理主键
     * @param appRefId 当前应用物理主键
     * @param memberVersion 当前成员生命周期版本
     * @param authVersion 当前成员授权版本
     * @return 命名空间隔离的 Redis 授权缓存键
     */
    public static String memberApiResourceCacheKey(String tenantRefId, String memberRefId,
                                                   String appRefId, long memberVersion, long authVersion) {
        if (blank(tenantRefId) || blank(memberRefId) || blank(appRefId)
                || memberVersion < 0 || authVersion < 0) {
            throw new IllegalArgumentException("Verified member authorization context is required");
        }
        return MEMBER_API_RESOURCE_CACHE_PREFIX + tenantRefId + ":" + memberRefId + ":"
                + appRefId + ":" + memberVersion + ":" + authVersion;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank() || value.indexOf(':') >= 0;
    }

    /** 用户 API 资源授权 Redis Set key 前缀。 */
    public static final String USER_API_RESOURCE_CACHE_PREFIX = "peach:security:user:api-resources:";

    /**
     * 构建按用户、租户和机构隔离的 API 资源缓存 Key。
     *
     * @param userId 用户 ID
     * @param tenantId 租户 ID
     * @param orgId 机构 ID
     * @return API 资源缓存 Key
     */
    public static String userApiResourceCacheKey(String userId, String tenantId, String orgId) {
        return USER_API_RESOURCE_CACHE_PREFIX + userId + ":" + tenantId + ":" + orgId;
    }
}
