package com.peach.gateway.core;

import com.peach.gateway.core.config.GatewayIamProperties;
import com.peach.gateway.core.security.GatewayIamDecisionVerifier;
import com.peach.gateway.core.security.GatewayIamHttpDecisionVerifier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 只有启用网关 IAM 且受信认证服务已部署后创建负载均衡的 HTTP 验证器。
 *
 * <p>复用 Spring Cloud LoadBalancer/Nacos 服务发现，不写死服务器主机或端口。
 * 若无服务实例、请求超时或 Token 校验失败，由验证器拒绝。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 11:55
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(WebClient.class)
@ConditionalOnProperty(prefix = "peach.gateway.iam", name = "enabled", havingValue = "true")
public class GatewayIamClientConfiguration {

    public static final String IAM_WEB_CLIENT_BUILDER = "gatewayIamWebClientBuilder";

    /**
     * 注册带服务发现的 WebClient 构建器，不直接访问浏览器传入的地址。
     */
    @Bean(IAM_WEB_CLIENT_BUILDER)
    @LoadBalanced
    public WebClient.Builder gatewayIamWebClientBuilder() {
        return WebClient.builder();
    }

    /**
     * 将查询 Auth 数据库事实源的验证器接入网关鉴权策略。
     */
    @Bean
    public GatewayIamDecisionVerifier gatewayIamDecisionVerifier(
            @Qualifier(IAM_WEB_CLIENT_BUILDER) WebClient.Builder builder,
            GatewayIamProperties properties) {
        return new GatewayIamHttpDecisionVerifier(builder, properties);
    }
}
