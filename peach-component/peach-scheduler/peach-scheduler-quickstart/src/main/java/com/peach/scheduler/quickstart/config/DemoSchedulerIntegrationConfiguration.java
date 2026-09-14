package com.peach.scheduler.quickstart.config;

import com.peach.scheduler.transport.ExecutionLeaseClient;
import com.peach.scheduler.transport.ExecutionResultReporter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Indexed;

/**
 * 本地集成桩：提供 Claim 与结果上报，使 {@code PeachJobExecutor} 能在无控制面 / 无 MQ 时装配。
 * {@code peach.scheduler.quickstart.local-mode=false} 时不注册，以便 RocketMQ Reporter 装配。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 19:00
 */
@Indexed
@Configuration
@ConditionalOnProperty(name = "peach.scheduler.quickstart.local-mode", havingValue = "true", matchIfMissing = true)
public class DemoSchedulerIntegrationConfiguration {

    /**
     * 可切换允许 / 拒绝的 Claim 桩；生产环境由 Feign 控制面客户端覆盖。
     */
    @Bean
    @ConditionalOnMissingBean(ExecutionLeaseClient.class)
    public DemoExecutionLeaseClient demoExecutionLeaseClient() {
        return new DemoExecutionLeaseClient();
    }

    /**
     * 内存结果上报桩；生产环境由 RocketMQ Outbox Reporter 覆盖。
     */
    @Bean
    @ConditionalOnMissingBean(ExecutionResultReporter.class)
    public DemoExecutionResultReporter demoExecutionResultReporter() {
        return new DemoExecutionResultReporter();
    }
}
