package com.peach.satoken.support;

import com.peach.common.audit.AuditContextProvider;
import com.peach.satoken.context.SecurityContextHolder;

/**
 * 基于 Sa-Token {@link SecurityContextHolder} 的审计上下文 Provider。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 16:13
 */
public final class SatokenAuditContextProvider implements AuditContextProvider {

    @Override
    public String currentUserId() {
        return SecurityContextHolder.currentUserId();
    }

    @Override
    public String currentTenantId() {
        return SecurityContextHolder.currentTenantId();
    }

    @Override
    public String currentOrgId() {
        return SecurityContextHolder.currentOrgId();
    }
}
