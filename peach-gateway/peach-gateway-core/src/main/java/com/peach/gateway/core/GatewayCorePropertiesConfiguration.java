package com.peach.gateway.core;

import com.peach.gateway.core.config.GatewayRiskControlProperties;
import com.peach.gateway.core.config.GatewaySaTokenProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Indexed;

/**
 * 网关核心配置属性。
 *
 * <p>该配置属于网关服务内部模块，由 {@code peach-gateway-launch} 的组件扫描加载。
 * 它集中启用 {@code peach-gateway-core} 暴露的配置属性，避免启动模块逐个声明
 * {@link EnableConfigurationProperties}。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/11 14:45
 */
@Indexed
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({
        GatewaySaTokenProperties.class,
        GatewayRiskControlProperties.class
})
public class GatewayCorePropertiesConfiguration {
}
