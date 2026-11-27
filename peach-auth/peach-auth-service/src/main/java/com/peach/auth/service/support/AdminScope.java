package com.peach.auth.service.support;

import com.peach.common.exception.BusinessException;
import com.peach.satoken.context.SecurityContextHolder;
import com.peach.satoken.context.UserContext;

/**
 * 管理端当前登录作用域；不接受请求体覆盖租户和机构。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 21:00
 */
public final class AdminScope {
    private AdminScope() { }
    /** 返回当前已认证身份。 */
    public static UserContext actor() {
        UserContext actor = SecurityContextHolder.get();
        if (actor == null || actor.getTenantId() == null || actor.getOrgId() == null || actor.getUserId() == null) throw new BusinessException("登录上下文缺失");
        return actor;
    }
}
