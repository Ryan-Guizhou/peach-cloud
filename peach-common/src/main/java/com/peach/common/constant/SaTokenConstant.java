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
}
