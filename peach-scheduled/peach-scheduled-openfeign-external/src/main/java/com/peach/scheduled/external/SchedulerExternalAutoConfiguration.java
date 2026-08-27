package com.peach.scheduled.external;

import org.springframework.stereotype.Indexed;

import com.peach.scheduler.config.PeachSchedulerProperties;
import com.peach.scheduler.core.JobRegistry;
import com.peach.scheduler.transport.ExecutionLeaseClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 调度模块相关说明。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@AutoConfiguration
@EnableFeignClients(clients = {SchedulerExecutionExternalClient.class, SchedulerHandlerExternalClient.class})
@EnableScheduling
@Indexed
public class SchedulerExternalAutoConfiguration {
    /**
     * 创建相关对象。
     */
    public SchedulerExternalAutoConfiguration() {
        // Intentionally empty.
    }

    /**
     * 创建相关对象。
     *
     * @return 返回结果
     */
    @Bean
    @ConditionalOnMissingBean
    public SchedulerExecutionExternalClientFallbackFactory schedulerExecutionExternalClientFallbackFactory() {
        return new SchedulerExecutionExternalClientFallbackFactory();
    }

    /**
     * 创建相关对象。
     *
     * @return 返回结果
     */
    @Bean
    @ConditionalOnMissingBean
    public SchedulerHandlerExternalClientFallbackFactory schedulerHandlerExternalClientFallbackFactory() {
        return new SchedulerHandlerExternalClientFallbackFactory();
    }

    /**
     * 创建相关对象。
     *
     * @return 返回结果
     * @param client Scheduler 控制面 Feign Client
     */
    @Bean
    @ConditionalOnMissingBean(ExecutionLeaseClient.class)
    public ExecutionLeaseClient executionLeaseClient(SchedulerExecutionExternalClient client) {
        return new FeignExecutionLeaseClient(client);
    }

    /**
     * 创建相关对象。
     *
     * @return 返回结果
     * @param registry 本地 Job Handler 注册表
     * @param client Scheduler 控制面 Feign Client
     * @param properties Scheduler 执行器配置
     */
    @Bean
    @ConditionalOnBean({JobRegistry.class, SchedulerHandlerExternalClient.class})
    public SchedulerHandlerRegistrationInitializer schedulerHandlerRegistrationInitializer(
            JobRegistry registry,
            SchedulerHandlerExternalClient client,
            PeachSchedulerProperties properties) {
        return new SchedulerHandlerRegistrationInitializer(registry, client, properties);
    }
}
