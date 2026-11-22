package com.peach.scheduler;

import com.peach.scheduler.config.PeachSchedulerProperties;
import com.peach.scheduler.core.JobRegistry;
import com.peach.scheduler.core.PeachJobExecutor;
import com.peach.scheduler.runtime.DefaultPeachJobExecutor;
import com.peach.scheduler.transport.ExecutionLeaseClient;
import com.peach.scheduler.transport.ExecutionResultReporter;
import com.peach.virtualthread.registry.VirtualExecutorRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 在 Handler 注册表与传输客户端装配完成后创建任务执行器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/21 13:10
 */
@AutoConfiguration(after = PeachSchedulerAutoConfiguration.class, afterName = {
        "com.peach.scheduled.external.SchedulerExternalAutoConfiguration",
        "com.peach.scheduler.rocket.PeachSchedulerRocketAutoConfiguration"})
@ConditionalOnClass(VirtualExecutorRegistry.class)
@ConditionalOnProperty(prefix = "peach.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PeachJobExecutorAutoConfiguration {

    /**
     * 创建默认执行器；业务自定义执行器存在时保留业务实现。
     *
     * @param registry Handler 注册表
     * @param virtualExecutorRegistry 受管虚拟线程执行器注册中心
     * @param leaseClient 控制面执行租约客户端
     * @param resultReporter 执行结果上报器
     * @param properties Scheduler 配置
     * @return 默认任务执行器
     */
    @Bean
    @ConditionalOnBean({VirtualExecutorRegistry.class, ExecutionLeaseClient.class, ExecutionResultReporter.class})
    @ConditionalOnMissingBean(PeachJobExecutor.class)
    public PeachJobExecutor peachJobExecutor(JobRegistry registry, VirtualExecutorRegistry virtualExecutorRegistry,
                                             ExecutionLeaseClient leaseClient, ExecutionResultReporter resultReporter,
                                             PeachSchedulerProperties properties) {
        return new DefaultPeachJobExecutor(registry, virtualExecutorRegistry, leaseClient, resultReporter, properties);
    }
}
