package com.peach.scheduler.example.config;

import org.springframework.stereotype.Indexed;

import com.peach.scheduler.transport.ExecutionLeaseClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 仅用于本地示例的集成实现。
 *
 * <p>调度模块相关说明。
 * 调度模块相关说明。
 * 调度模块相关说明。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Configuration
@Indexed
public class DemoSchedulerIntegrationConfiguration {

    /**
     * 创建相关对象。
     */
    public DemoSchedulerIntegrationConfiguration() {
    }

    /**
     * 创建相关对象。
     *
     * @return 返回结果
     */
    @Bean
    @ConditionalOnMissingBean(ExecutionLeaseClient.class)
    public ExecutionLeaseClient demoExecutionLeaseClient() {
        return (executionId, executorInstance) -> true;
    }
}
