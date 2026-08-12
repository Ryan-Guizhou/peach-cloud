package com.peach.satoken.constant;

/**
 * Sa-Token 相关常量。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/10/10 15:30
 */
public interface SatokenConstant {

    /**
     * 用户上下文 Hash 缓存 Key 前缀。
     */
    String USER_PROFILE_CACHE_PREFIX = "peach:security:user:profile:";

    /**
     * 用户上下文 Hash 字段：用户ID。
     */
    String USER_PROFILE_FIELD_USER_ID = "userId";

    /**
     * 用户上下文 Hash 字段：用户编码。
     */
    String USER_PROFILE_FIELD_USER_CODE = "userCode";

    /**
     * 用户上下文 Hash 字段：用户名称。
     */
    String USER_PROFILE_FIELD_USER_NAME = "userName";

    /**
     * 用户上下文 Hash 字段：租户ID。
     */
    String USER_PROFILE_FIELD_TENANT_ID = "tenantId";

    /**
     * 用户上下文 Hash 字段：租户名称。
     */
    String USER_PROFILE_FIELD_TENANT_NAME = "tenantName";

    /**
     * 用户上下文 Hash 字段：组织ID。
     */
    String USER_PROFILE_FIELD_ORG_ID = "orgId";

    /**
     * 用户上下文 Hash 字段：组织编码。
     */
    String USER_PROFILE_FIELD_ORG_CODE = "orgCode";

    /**
     * 用户上下文 Hash 字段：组织名称。
     */
    String USER_PROFILE_FIELD_ORG_NAME = "orgName";

    /**
     * 用户上下文 Hash 字段：会计期间。
     */
    String USER_PROFILE_FIELD_FISCAL = "fiscal";

    /**
     * 用户上下文 Hash 字段：语言。
     */
    String USER_PROFILE_FIELD_LANG = "lang";

    /**
     * 用户上下文 Hash 字段：上下文版本号。
     */
    String USER_PROFILE_FIELD_CONTEXT_VERSION = "contextVersion";

}
