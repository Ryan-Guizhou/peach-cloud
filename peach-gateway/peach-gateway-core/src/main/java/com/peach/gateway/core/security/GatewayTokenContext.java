package com.peach.gateway.core.security;

/**
 * 当前 Sa-Token 凭证对应的租户业务上下文。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/06
 */
public record GatewayTokenContext(String tenantId, String orgId, String fiscal) {
}
