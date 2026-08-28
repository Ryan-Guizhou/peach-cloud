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
import com.peach.threadpool.manager.ThreadPoolManager;
import java.util.List;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Peach调度自动配置。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@AutoConfiguration
@EnableConfigurationProperties(PeachSchedulerProperties.class)
@ConditionalOnProperty(prefix = "peach.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
@Indexed
public class PeachSchedulerAutoConfiguration {

    /**
     * 创建实例。
     *
     * @return 执行结果。
     */
    @Bean
    @ConditionalOnMissingBean
    public JobRegistry jobRegistry() {
        return new JobRegistry();
    }

    /**
     * 创建实例。
     * @param registry registry。
     * @param handlers handlers。
     * @return 执行结果。
     */
    @Bean
    @ConditionalOnMissingBean
    public PeachJobRegistrationInitializer peachJobRegistrationInitializer(JobRegistry registry, List<JobHandler> handlers) {
        return new PeachJobRegistrationInitializer(registry, handlers);
    }

    /**
     * 创建实例。
     * @param registry registry。
     * @param threadPoolManager thread Pool Manager。
     * @param leaseClient lease Client。
     * @param resultReporter result Reporter。
     * @param properties properties。
     * @return 执行结果。
     */
    @Bean
    @ConditionalOnBean({ThreadPoolManager.class, ExecutionLeaseClient.class, ExecutionResultReporter.class})
    @ConditionalOnMissingBean(PeachJobExecutor.class)
    public PeachJobExecutor peachJobExecutor(JobRegistry registry, ThreadPoolManager threadPoolManager,
                                             ExecutionLeaseClient leaseClient, ExecutionResultReporter resultReporter,
                                             PeachSchedulerProperties properties) {
        return new DefaultPeachJobExecutor(registry, threadPoolManager, leaseClient, resultReporter, properties);
    }
}
