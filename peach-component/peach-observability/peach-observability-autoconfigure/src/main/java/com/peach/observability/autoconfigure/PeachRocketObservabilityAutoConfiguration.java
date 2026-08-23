package com.peach.observability.autoconfigure;

import com.peach.observability.messaging.MicrometerMqTraceContextPropagator;
import com.peach.rocket.context.MqTraceContextPropagator;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * RocketMQ 可观测性自动配置。
 *
 * <p>仅当 RocketMQ 传播 SPI 和 Micrometer Tracing 同时存在时生效，不会让 RocketMQ 模块
 * 直接依赖 OpenTelemetry 或其他具体 Tracing 实现。</p>
 */
@AutoConfiguration
@AutoConfigureAfter(PeachObservabilityAutoConfiguration.class)
@AutoConfigureBefore(name = "com.peach.rocket.autoconfigure.PeachRocketAutoConfigure")
@ConditionalOnClass({MqTraceContextPropagator.class, Tracer.class, Propagator.class})
@ConditionalOnProperty(prefix = "peach.observability", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PeachRocketObservabilityAutoConfiguration {

    /**
     * 创建 RocketMQ 的 Micrometer 链路上下文传播器。
     *
     * @param tracer Micrometer Tracer
     * @param propagator 标准链路传播器
     * @return MQ 链路上下文传播器
     */
    @Bean
    @ConditionalOnMissingBean(MqTraceContextPropagator.class)
    public MqTraceContextPropagator mqTraceContextPropagator(Tracer tracer, Propagator propagator) {
        return new MicrometerMqTraceContextPropagator(tracer, propagator);
    }
}
