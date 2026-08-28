package com.peach.observability.autoconfigure;

import com.peach.observability.config.PeachObservabilityProperties;
import com.peach.observability.core.RequestIdGenerator;
import com.peach.observability.core.RequestIdResolver;
import com.peach.observability.core.UuidRequestIdGenerator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * PeachObservability自动配置。
 * <p>提供请求关联标识的生成和解析基础设施。指标、Trace 和 OTLP 导出由 starter 引入的
 * Spring Boot Actuator、Micrometer 和 OpenTelemetry 官方自动配置负责。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
@AutoConfiguration
@EnableConfigurationProperties(PeachObservabilityProperties.class)
@ConditionalOnProperty(prefix = "peach.observability", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PeachObservabilityAutoConfiguration {

    /**
     * 创建默认请求 ID 生成器。
     *
     * @return UUID 请求 ID 生成器
     */
    @Bean
    @ConditionalOnMissingBean
    public RequestIdGenerator requestIdGenerator() {
        return new UuidRequestIdGenerator();
    }

    /**
     * 创建请求 ID 解析器。
     *
     * @param properties 可观测性配置
     * @param generator 请求 ID 生成器
     * @return 请求 ID 解析器
     */
    @Bean
    @ConditionalOnMissingBean
    public RequestIdResolver requestIdResolver(PeachObservabilityProperties properties,
                                               RequestIdGenerator generator) {
        return new RequestIdResolver(properties.getRequestId(), generator);
    }
}
