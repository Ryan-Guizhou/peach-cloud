package com.peach.common.audit;

/**
 * 审计上下文读取契约，由安全模块在启动时注册具体实现。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 16:13
 */
public interface AuditContextProvider {

    /**
     * 当前操作用户 ID。
     *
     * @return 用户 ID；无上下文时返回 {@code null}
     */
    String currentUserId();

    /**
     * 当前租户 ID。
     *
     * @return 租户 ID；无上下文时返回 {@code null}
     */
    String currentTenantId();

    /**
     * 当前组织 ID。
     *
     * @return 组织 ID；无上下文时返回 {@code null}
     */
    String currentOrgId();
}
