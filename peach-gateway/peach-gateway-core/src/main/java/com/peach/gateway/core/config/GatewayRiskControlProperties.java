package com.peach.gateway.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 网关RiskControl配置属性。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/11 14:45
 */
@Data
@ConfigurationProperties(prefix = "peach.gateway.risk-control")
public class GatewayRiskControlProperties {

    /**
     * 是否启用网关风控过滤器。
     */
    private boolean enabled;

    /**
     * 请求原始 URI 最大允许长度。
     */
    private int maxUriLength = 2048;

    /**
     * 请求头最大允许数量。
     */
    private int maxHeaderCount = 100;

    /**
     * 静态客户端地址黑名单，多个值使用英文逗号分隔。
     */
    private String blockedIps;

    /**
     * 静态 User-Agent 黑名单，多个值使用英文逗号分隔。
     */
    private String blockedUserAgents;
}
