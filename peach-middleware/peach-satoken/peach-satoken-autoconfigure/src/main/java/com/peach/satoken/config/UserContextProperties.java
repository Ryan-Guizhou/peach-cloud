package com.peach.satoken.config;

import com.peach.satoken.security.SatokenEndpointRule;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

/**
 * 用户上下文配置属性。
 * <p>该配置只作用于引入 `peach-satoken-starter` 的 Servlet 业务服务。
 * 公开端点会跳过未登录用户上下文恢复，同时也会被 Same-Token 拦截器放行。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/10/10 15:30
 */
@Data
@ConfigurationProperties(prefix = "peach.satoken.user-context")
public class UserContextProperties {

    /**
     * 是否启用当前用户上下文恢复过滤器。
     */
    private boolean enabled = true;

    /**
     * 业务服务侧公开端点列表。
     *
     * <p>命中该列表的请求允许未登录访问，并跳过 Same-Token 校验。网关路径被
     * StripPrefix 后进入业务服务，因此这里应配置业务服务真实 Servlet 路径。</p>
     */
    private List<SatokenEndpointRule> publicEndpoints = Arrays.asList(
            SatokenEndpointRule.of("OPTIONS", "/**"),
            SatokenEndpointRule.any("/login"),
            SatokenEndpointRule.any("/logout"),
            SatokenEndpointRule.any("/register"),
            SatokenEndpointRule.any("/getCaptcha"),
            SatokenEndpointRule.any("/checkCaptcha"),
            SatokenEndpointRule.any("/init"),
            SatokenEndpointRule.any("/doc.html"),
            SatokenEndpointRule.any("/swagger-resources"),
            SatokenEndpointRule.any("/swagger-resources/**"),
            SatokenEndpointRule.any("/webjars"),
            SatokenEndpointRule.any("/webjars/**"),
            SatokenEndpointRule.any("/v3/api-docs"),
            SatokenEndpointRule.any("/v3/api-docs/**"),
            SatokenEndpointRule.any("/v2/api-docs"),
            SatokenEndpointRule.any("/v2/api-docs/**"),
            SatokenEndpointRule.any("/actuator"),
            SatokenEndpointRule.any("/actuator/**"),
            SatokenEndpointRule.any("/health"),
            SatokenEndpointRule.any("health"),
            SatokenEndpointRule.any("/sse"),
            SatokenEndpointRule.any("/sse/**"),
            SatokenEndpointRule.any("/favicon.ico"),
            SatokenEndpointRule.of("POST", "/api/auth/login"),
            SatokenEndpointRule.of("POST", "/api/auth/logout"),
            SatokenEndpointRule.of("POST", "/api/auth/register"),
            SatokenEndpointRule.of("POST", "/api/auth/forget"),
            SatokenEndpointRule.of("POST", "/api/auth/reset"),
            SatokenEndpointRule.of("POST", "/api/auth/getCaptcha"),
            SatokenEndpointRule.of("POST", "/api/auth/checkCaptcha"),
            SatokenEndpointRule.of("POST", "/api/auth/init"),
            SatokenEndpointRule.of("GET", "/api/auth/rsa-public-key"),
            SatokenEndpointRule.any("/api/doc.html"),
            SatokenEndpointRule.any("/api/swagger-resources"),
            SatokenEndpointRule.any("/api/swagger-resources/**"),
            SatokenEndpointRule.any("/api/webjars"),
            SatokenEndpointRule.any("/api/webjars/**"),
            SatokenEndpointRule.any("/api/v3/api-docs"),
            SatokenEndpointRule.any("/api/v3/api-docs/**"),
            SatokenEndpointRule.any("/api/v2/api-docs"),
            SatokenEndpointRule.any("/api/v2/api-docs/**"),
            SatokenEndpointRule.any("/api/actuator"),
            SatokenEndpointRule.any("/api/actuator/**"),
            SatokenEndpointRule.any("/api/health"),
            SatokenEndpointRule.any("/api/sse"),
            SatokenEndpointRule.any("/api/sse/**"),
            SatokenEndpointRule.any("/api/favicon.ico"),
            SatokenEndpointRule.of("GET", "/api/auth/v3/api-docs"),
            SatokenEndpointRule.of("GET", "/api/file/v3/api-docs"),
            SatokenEndpointRule.of("GET", "/api/message/v3/api-docs"),
            SatokenEndpointRule.of("GET", "/api/setting/v3/api-docs"),
            SatokenEndpointRule.of("GET", "/api/generator/v3/api-docs"),
            SatokenEndpointRule.of("GET", "/api/monitor/v3/api-docs"),
            SatokenEndpointRule.of("GET", "/api/monitor/actuator/**"),
            SatokenEndpointRule.of("GET", "/api/monitor/health")
    );

}
