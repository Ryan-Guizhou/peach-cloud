package com.peach.auth.util;

/**
 * 仅供 Auth 前置过滤器向同一 Servlet 请求传递已经验证的 SaaS 身份。
 *
 * <p>这些值必须从服务端真实 Token 和数据库会话获得，不能来自 HTTP 请求头
 * 或客户端参数；请求结束时必须由写入方清理。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/20 13:50
 */
public final class UserOperLogTrustedContext {

    public static final String TENANT_REF_ID =
            UserOperLogTrustedContext.class.getName() + ".tenantRefId";

    public static final String APP_REF_ID =
            UserOperLogTrustedContext.class.getName() + ".appRefId";

    public static final String MEMBER_REF_ID =
            UserOperLogTrustedContext.class.getName() + ".memberRefId";

    private UserOperLogTrustedContext() {
        throw new IllegalStateException("Utility class");
    }
}
