package com.peach.observability.autoconfigure;

import com.peach.observability.messaging.MicrometerMqMetrics;
import com.peach.rocket.metrics.MqMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 可选 RocketMQ Micrometer 指标自动配置，与 Tracing 是否启用互不依赖。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13
 */
@AutoConfiguration
@AutoConfigureAfter(name = "org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration")
@AutoConfigureBefore(name = "com.peach.rocket.autoconfigure.PeachRocketAutoConfigure")
@ConditionalOnClass({MqMetrics.class, MeterRegistry.class})
@ConditionalOnBean(MeterRegistry.class)
@ConditionalOnProperty(prefix = "peach.observability", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class PeachRocketMetricsAutoConfiguration {

    /**
     * 在监控端与 RocketMQ SPI 均存在时注册低基数指标收集器。
     *
     * @param registry 既有 Micrometer 注册器
     * @return RocketMQ 指标收集 SPI
     */
    @Bean
    @ConditionalOnMissingBean(MqMetrics.class)
    public MqMetrics mqMetrics(MeterRegistry registry) {
        return new MicrometerMqMetrics(registry);
    }
}
