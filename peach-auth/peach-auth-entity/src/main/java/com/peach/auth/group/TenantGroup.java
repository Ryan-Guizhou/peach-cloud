package com.peach.auth.group;

import jakarta.validation.groups.Default;
/**
 * 租户管理。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/21 18:00
 */
public final class TenantGroup {
    private TenantGroup() {}
    public interface Create extends Default {}
    public interface Update extends Default {}
}
