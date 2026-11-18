package com.peach.fileservice.service.support;

import org.springframework.util.StringUtils;

/**
 * 文件领域已有业务租户/机构边界的最小安全保护器。
 *
 * <p>当前上下文必须由服务端可信审计上下文提供，不接受上传 DTO、
 * 文件查询参数或 HTTP 自报的租户、机构标识。
 * 在任何文件详情、签名下载、变更和上传会话访问前检查数据库归属。
 * 仅作为业务标识阶段的防越权止血，不代表已经具备新的 SaaS 物理租户、应用与对象级授权。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 16:15
 */
public final class FileRecordScopeGuard {

    private FileRecordScopeGuard() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 确保记录与服务器真实当前业务租户、机构完全一致。
     *
     * @param trustedTenantId 服务器端可信租户业务标识
     * @param trustedOrgId 服务器端可信机构业务标识
     * @param storedTenantId 数据库记录归属租户业务标识
     * @param storedOrgId 数据库记录归属机构业务标识
     */
    public static void requireSameScope(String trustedTenantId, String trustedOrgId,
                                        String storedTenantId, String storedOrgId) {
        if (!StringUtils.hasText(trustedTenantId) || !StringUtils.hasText(trustedOrgId)
                || !StringUtils.hasText(storedTenantId) || !StringUtils.hasText(storedOrgId)
                || !trustedTenantId.equals(storedTenantId) || !trustedOrgId.equals(storedOrgId)) {
            throw new SecurityException("Current principal is not authorized for this file scope");
        }
    }
}
