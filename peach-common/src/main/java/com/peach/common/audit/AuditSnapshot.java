package com.peach.common.audit;

/**
 * 一次审计操作所需的上下文快照。
 *
 * @param userId   操作用户 ID
 * @param tenantId 租户 ID
 * @param orgId    组织 ID
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 16:13
 */
public record AuditSnapshot(String userId, String tenantId, String orgId) {

    /**
     * 从当前注册的 {@link AuditContextProvider} 构建快照。
     *
     * @return 审计快照
     */
    public static AuditSnapshot fromContext() {
        return new AuditSnapshot(
                AuditContext.currentUserId().orElse(null),
                AuditContext.currentTenantId().orElse(null),
                AuditContext.currentOrgId().orElse(null)
        );
    }
}
