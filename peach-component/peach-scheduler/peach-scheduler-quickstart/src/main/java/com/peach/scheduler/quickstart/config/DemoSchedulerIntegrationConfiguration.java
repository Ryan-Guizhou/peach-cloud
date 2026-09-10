package com.peach.scheduler.quickstart.config;

import org.springframework.stereotype.Indexed;

import com.peach.scheduler.transport.ExecutionLeaseClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 调度示例集成配置，提供 quickstart 场景所需的默认执行租约实现。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Configuration
@Indexed
public class DemoSchedulerIntegrationConfiguration {

    /**
     * 创建示例集成配置。
     */
    public DemoSchedulerIntegrationConfiguration() {
        // Intentionally empty.
    }

    /**
     * 创建始终允许执行的示例租约客户端。
     *
     * @return 示例执行租约客户端。
     */
    @Bean
    @ConditionalOnMissingBean(ExecutionLeaseClient.class)
    public ExecutionLeaseClient demoExecutionLeaseClient() {
        return (executionId, executorInstance) -> true;
    }
}
