package com.peach.auth.event;

/**
 * 租户停用、注销或逻辑删除后触发的会话失效事件。
 * @param tenantId 不再允许访问的租户业务 ID
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/21 18:00
 */
public record TenantUnavailableEvent(String tenantId) { }
