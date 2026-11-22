package com.peach.auth.service.support;

import com.peach.common.exception.BusinessException;
import com.peach.satoken.context.SecurityContextHolder;
import java.util.Objects;

/**
 * 功能与资源定义的当前租户边界。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 12:00
 */
public final class DefinitionScope {
    private DefinitionScope() { }

    /** 从已认证上下文读取租户，禁止使用客户端传入的租户覆盖。 */
    public static String tenantId() {
        String tenant = SecurityContextHolder.currentTenantId();
        if (tenant == null) throw new BusinessException("登录租户上下文缺失");
        return tenant;
    }

    /** 按主键读取后必须验证租户归属，避免跨租户维护。 */
    public static void requireTenant(String tenant) {
        if (!Objects.equals(tenantId(), tenant)) throw new BusinessException("记录不存在或不可访问");
    }
}
