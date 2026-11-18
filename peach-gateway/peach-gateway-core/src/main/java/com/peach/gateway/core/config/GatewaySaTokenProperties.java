package com.peach.gateway.core.config;

import com.peach.gateway.core.security.GatewaySecurityEndpointRule;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

/**
 * 网关Sa令牌配置属性。
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
            GatewaySecurityEndpointRule.of("POST", "/auth/login"),
            // 登录初始化仅返回公开登录配置与 RSA 公钥，不执行系统管理员引导。
            GatewaySecurityEndpointRule.of("POST", "/auth/init"),
            GatewaySecurityEndpointRule.of("POST", "/auth/getCaptcha"),
            GatewaySecurityEndpointRule.of("POST", "/auth/checkCaptcha"),
            GatewaySecurityEndpointRule.of("GET", "/auth/rsa-public-key"),
            GatewaySecurityEndpointRule.of("POST", "/api/auth/login"),
            GatewaySecurityEndpointRule.of("POST", "/api/auth/init"),
            GatewaySecurityEndpointRule.of("POST", "/api/auth/getCaptcha"),
            GatewaySecurityEndpointRule.of("POST", "/api/auth/checkCaptcha"),
            GatewaySecurityEndpointRule.of("GET", "/api/auth/rsa-public-key")
    );
}
