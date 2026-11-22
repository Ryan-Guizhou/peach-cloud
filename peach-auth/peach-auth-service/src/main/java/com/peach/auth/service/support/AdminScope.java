package com.peach.auth.service.support;

import com.peach.common.exception.BusinessException;
import com.peach.satoken.context.SecurityContextHolder;
import com.peach.satoken.context.UserContext;

/**
 * 管理端当前登录作用域；不接受请求体覆盖租户、机构、年度和应用。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 21:00
 */
public final class AdminScope {
    public static final String APP_ID = "f73b300578a5436d82ec7fca2c07c284";
    private AdminScope() { }
    /** 返回当前已认证身份。 */
    public static UserContext actor() {
        UserContext actor = SecurityContextHolder.get();
        if (actor == null || actor.getTenantId() == null || actor.getOrgId() == null || actor.getUserId() == null || actor.getFiscal() == null) throw new BusinessException("登录上下文缺失");
        return actor;
    }
    /** 当前会计年度。 */
    public static int fiscal() {
        try { return Integer.parseInt(actor().getFiscal()); }
        catch (NumberFormatException ex) { throw new BusinessException("登录年度无效"); }
    }
}
