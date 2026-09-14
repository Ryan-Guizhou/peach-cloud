package com.peach.scheduler;

import org.springframework.stereotype.Indexed;

import com.peach.scheduler.config.PeachSchedulerProperties;
import com.peach.scheduler.core.JobHandler;
import com.peach.scheduler.core.JobRegistry;
import com.peach.scheduler.core.PeachJobExecutor;
import com.peach.scheduler.runtime.DefaultPeachJobExecutor;
import com.peach.scheduler.runtime.PeachJobRegistrationInitializer;
import com.peach.scheduler.transport.ExecutionLeaseClient;
import com.peach.scheduler.transport.ExecutionResultReporter;
import com.peach.virtualthread.registry.VirtualExecutorRegistry;
import java.util.List;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Peach Scheduler 执行侧自动配置。
 *
 * <p>该配置只负责业务应用中的 Handler 注册、执行器编排 Bean 和执行结果上报入口装配。
 * 业务 Handler 的阻塞 IO 执行通过 {@link VirtualExecutorRegistry} 中名为 {@code scheduler}
 * 的虚拟线程业务组隔离；Quartz 触发和控制面状态管理不在本配置中完成。必须排在
 * {@code PeachVirtualThreadAutoConfiguration} 之后，否则 {@code @ConditionalOnBean(VirtualExecutorRegistry)}
 * 会在注册中心创建前求值，导致 {@link PeachJobExecutor} 缺失。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@AutoConfiguration(afterName = "com.peach.virtualthread.autoconfigure.PeachVirtualThreadAutoConfiguration")
@EnableConfigurationProperties(PeachSchedulerProperties.class)
@ConditionalOnProperty(prefix = "peach.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(VirtualExecutorRegistry.class)
@Indexed
public class PeachSchedulerAutoConfiguration {

    /**
     * 创建业务 Handler 注册表。
     *
     * @return 业务 Handler 注册表
     */
    @Bean
    @ConditionalOnMissingBean
    public JobRegistry jobRegistry() {
        return new JobRegistry();
    }

    /**
     * 创建启动期 Handler 注册初始化器。
     *
     * @param registry Handler 注册表
     * @param handlers Spring 容器中发现的 Handler Bean
     * @return Handler 注册初始化器
     */
    @Bean
    @ConditionalOnMissingBean
    public PeachJobRegistrationInitializer peachJobRegistrationInitializer(JobRegistry registry, List<JobHandler> handlers) {
        return new PeachJobRegistrationInitializer(registry, handlers);
    }

    /**
     * 创建默认任务执行器。
     *
     * <p>只有执行租约客户端、结果上报器和虚拟线程注册中心同时存在时才创建默认执行器。
     * 业务可以声明自己的 {@link PeachJobExecutor} Bean 覆盖该默认实现。</p>
     *
     * @param registry Handler 注册表
     * @param virtualExecutorRegistry 虚拟线程执行器注册中心
     * @param leaseClient 执行租约客户端
     * @param resultReporter 执行结果上报器
     * @param properties Scheduler 配置属性
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
