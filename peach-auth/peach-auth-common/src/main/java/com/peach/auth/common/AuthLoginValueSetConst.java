package com.peach.auth.common;

/**
 * 登录初始化相关值集编码与项编码。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/27 11:10
 */
public final class AuthLoginValueSetConst {

    public static final String VALUE_SET_CODE = "AUTH_LOGIN_CONFIG";

    public static final String SYSTEM_NAME = "SYSTEM_NAME";

    public static final String SYSTEM_DESCRIPTION = "SYSTEM_DESCRIPTION";

    public static final String ENCRYPTION_ALGORITHM = "ENCRYPTION_ALGORITHM";

    public static final String CAPTCHA_ENABLED = "CAPTCHA_ENABLED";

    public static final String CAPTCHA_TYPE = "CAPTCHA_TYPE";

    private AuthLoginValueSetConst() {
        throw new IllegalStateException("Utility class");
    }
}
