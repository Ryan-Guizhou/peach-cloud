package com.peach.gateway.core.config;

import com.peach.gateway.core.security.GatewaySecurityEndpointRule;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

/**
 * 网关 Sa-Token 配置属性。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/11 14:45
 */
@Data
@ConfigurationProperties(prefix = "peach.gateway.satoken")
public class GatewaySaTokenProperties {

    /**
     * 是否启用网关认证和网关 Sa-Token 定制能力。
     */
    private boolean enabled = true;

    /**
     * 是否为下游服务调用注入 Sa-Token Same-Token 凭证。
     */
    private boolean injectSameToken = true;

    /**
     * 是否覆盖 Sa-Token token 生成策略。
     */
    private boolean tokenStrategyEnabled = true;

    /**
     * 是否记录公开端点放行日志。
     */
    private boolean logPath = true;

    /**
     * 是否启用网关 API 资源权限校验。
     */
    private boolean apiPermissionEnabled = true;

    /**
     * 网关认证和风控需要跳过的公开端点列表。
     */
    private List<GatewaySecurityEndpointRule> publicEndpoints = Arrays.asList(
            GatewaySecurityEndpointRule.of("OPTIONS", "/**"),
            GatewaySecurityEndpointRule.any("/login"),
            GatewaySecurityEndpointRule.any("/logout"),
            GatewaySecurityEndpointRule.any("/register"),
            GatewaySecurityEndpointRule.any("/getCaptcha"),
            GatewaySecurityEndpointRule.any("/checkCaptcha"),
            GatewaySecurityEndpointRule.any("/init"),
            GatewaySecurityEndpointRule.any("/doc.html"),
            GatewaySecurityEndpointRule.any("/swagger-resources"),
            GatewaySecurityEndpointRule.any("/swagger-resources/**"),
            GatewaySecurityEndpointRule.any("/webjars"),
            GatewaySecurityEndpointRule.any("/webjars/**"),
            GatewaySecurityEndpointRule.any("/v3/api-docs"),
            GatewaySecurityEndpointRule.any("/v3/api-docs/**"),
            GatewaySecurityEndpointRule.any("/v2/api-docs"),
            GatewaySecurityEndpointRule.any("/v2/api-docs/**"),
            GatewaySecurityEndpointRule.any("/actuator"),
            GatewaySecurityEndpointRule.any("/actuator/**"),
            GatewaySecurityEndpointRule.any("/health"),
            GatewaySecurityEndpointRule.any("health"),
            GatewaySecurityEndpointRule.any("/sse"),
            GatewaySecurityEndpointRule.any("/sse/**"),
            GatewaySecurityEndpointRule.any("/favicon.ico"),
            GatewaySecurityEndpointRule.of("POST", "/api/auth/login"),
            GatewaySecurityEndpointRule.of("POST", "/api/auth/logout"),
            GatewaySecurityEndpointRule.of("POST", "/api/auth/register"),
            GatewaySecurityEndpointRule.of("POST", "/api/auth/forget"),
            GatewaySecurityEndpointRule.of("POST", "/api/auth/reset"),
            GatewaySecurityEndpointRule.of("POST", "/api/auth/getCaptcha"),
            GatewaySecurityEndpointRule.of("POST", "/api/auth/checkCaptcha"),
            GatewaySecurityEndpointRule.of("POST", "/api/auth/init"),
            GatewaySecurityEndpointRule.of("GET", "/api/auth/rsa-public-key"),
            GatewaySecurityEndpointRule.any("/api/doc.html"),
            GatewaySecurityEndpointRule.any("/api/swagger-resources"),
            GatewaySecurityEndpointRule.any("/api/swagger-resources/**"),
            GatewaySecurityEndpointRule.any("/api/webjars"),
            GatewaySecurityEndpointRule.any("/api/webjars/**"),
            GatewaySecurityEndpointRule.any("/api/v3/api-docs"),
            GatewaySecurityEndpointRule.any("/api/v3/api-docs/**"),
            GatewaySecurityEndpointRule.any("/api/v2/api-docs"),
            GatewaySecurityEndpointRule.any("/api/v2/api-docs/**"),
            GatewaySecurityEndpointRule.any("/api/actuator"),
            GatewaySecurityEndpointRule.any("/api/actuator/**"),
            GatewaySecurityEndpointRule.any("/api/health"),
            GatewaySecurityEndpointRule.any("/api/sse"),
            GatewaySecurityEndpointRule.any("/api/sse/**"),
            GatewaySecurityEndpointRule.any("/api/favicon.ico"),
            GatewaySecurityEndpointRule.of("GET", "/api/auth/v3/api-docs"),
            GatewaySecurityEndpointRule.of("GET", "/api/file/v3/api-docs"),
            GatewaySecurityEndpointRule.of("GET", "/api/message/v3/api-docs"),
            GatewaySecurityEndpointRule.of("GET", "/api/setting/v3/api-docs"),
            GatewaySecurityEndpointRule.of("GET", "/api/generator/v3/api-docs"),
            GatewaySecurityEndpointRule.of("GET", "/api/monitor/v3/api-docs"),
            GatewaySecurityEndpointRule.of("GET", "/api/monitor/actuator/**"),
            GatewaySecurityEndpointRule.of("GET", "/api/monitor/health")
    );
}
