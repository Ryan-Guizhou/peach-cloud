package com.peach.common.audit;

import com.peach.common.util.StringUtil;

import java.util.Optional;

/**
 * 审计上下文访问入口，避免 {@code peach-common} 直接依赖具体安全实现。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 16:13
 */
public final class AuditContext {

    private static volatile AuditContextProvider provider;

    private AuditContext() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 注册审计上下文 Provider，通常由 Sa-Token 自动配置完成。
     *
     * @param auditProvider Provider 实现
     */
    public static void register(AuditContextProvider auditProvider) {
        if (auditProvider == null) {
            throw new IllegalArgumentException("auditProvider must not be null");
        }
        provider = auditProvider;
    }

    /**
     * 读取当前用户 ID。
     *
     * @return 非空用户 ID
     */
    public static Optional<String> currentUserId() {
        return optionalValue(provider == null ? null : provider.currentUserId());
    }

    /**
     * 读取当前租户 ID。
     *
     * @return 非空租户 ID
     */
    public static Optional<String> currentTenantId() {
        return optionalValue(provider == null ? null : provider.currentTenantId());
    }

    /**
     * 读取当前组织 ID。
     *
     * @return 非空组织 ID
     */
    public static Optional<String> currentOrgId() {
        return optionalValue(provider == null ? null : provider.currentOrgId());
    }

    /**
     * 获取当前租户 ID，缺失时抛出异常。
     *
     * @return 租户 ID
     * @throws IllegalStateException 当前租户上下文缺失时抛出
     */
    public static String requireTenantId() {
        return currentTenantId().orElseThrow(() -> new IllegalStateException("Current tenant context is missing"));
    }

    /**
     * 获取当前组织 ID，缺失时抛出异常。
     *
     * @return 组织 ID
     * @throws IllegalStateException 当前组织上下文缺失时抛出
     */
    public static String requireOrgId() {
        return currentOrgId().orElseThrow(() -> new IllegalStateException("Current organization context is missing"));
    }

    /**
     * 测试或容器重启场景下清理 Provider。
     */
    static void clear() {
        provider = null;
    }

    private static Optional<String> optionalValue(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }
        return Optional.of(value.trim());
    }
}
