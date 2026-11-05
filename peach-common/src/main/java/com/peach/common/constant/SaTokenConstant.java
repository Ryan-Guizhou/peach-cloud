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
}
