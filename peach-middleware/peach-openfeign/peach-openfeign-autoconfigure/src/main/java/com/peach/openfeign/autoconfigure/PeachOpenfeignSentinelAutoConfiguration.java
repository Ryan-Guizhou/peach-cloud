package com.peach.openfeign.autoconfigure;

import com.peach.openfeign.config.PeachOpenfeignProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Indexed;

import jakarta.annotation.PostConstruct;

/**
 * Peach OpenFeign Sentinel 治理自动配置。
 *
 * <p>该配置不直接创建 Sentinel 规则数据源，规则数据源由
 * Spring Cloud Alibaba Sentinel 的 {@code spring.cloud.sentinel.datasource}
 * 配置装配；这里负责校验 Feign Sentinel 开关与 Peach 配置是否一致。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 15:30
 */
@Slf4j
@Indexed
@AutoConfiguration
@AutoConfigureAfter(PeachOpenfeignAutoConfiguration.class)
@ConditionalOnClass(name = "com.alibaba.cloud.sentinel.feign.SentinelFeign")
@ConditionalOnProperty(prefix = "peach.openfeign.sentinel", name = "enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(PeachOpenfeignProperties.class)
public class PeachOpenfeignSentinelAutoConfiguration {

    private final PeachOpenfeignProperties properties;

    private final Environment environment;

    public PeachOpenfeignSentinelAutoConfiguration(PeachOpenfeignProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @PostConstruct
    public void logSentinelConfig() {
        boolean feignSentinelEnabled = environment.getProperty("feign.sentinel.enabled", Boolean.class, Boolean.FALSE);
        boolean feignCircuitBreakerEnabled = environment.getProperty("feign.circuitbreaker.enabled", Boolean.class,
                Boolean.FALSE);
        if (!feignSentinelEnabled) {
            log.warn("[PeachFeign] sentinel enabled but feign.sentinel.enabled=false");
            return;
        }
        log.info("[PeachFeign] sentinel initialized circuitBreakerEnabled={} datasourceType={} flowDataId={} degradeDataId={} groupId={}",
                feignCircuitBreakerEnabled,
                properties.getSentinel().getDatasourceType(),
                properties.getSentinel().getFlowDataId(),
                properties.getSentinel().getDegradeDataId(),
                properties.getSentinel().getGroupId());
    }
}
