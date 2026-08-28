package com.peach.scheduler.config;

import org.springframework.stereotype.Indexed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peach.rocket.outbox.MqOutboxPublisher;
import com.peach.scheduler.dao.SchedulerExecutionAttemptDao;
import com.peach.scheduler.dao.SchedulerExecutionDao;
import com.peach.scheduler.dao.SchedulerHandlerDao;
import com.peach.scheduler.dao.SchedulerJobDao;
import com.peach.scheduler.dao.SchedulerJobVersionDao;
import com.peach.scheduler.dao.SchedulerOperationLogDao;
import com.peach.scheduler.dao.SchedulerStateLogDao;
import com.peach.scheduler.dispatch.JobDispatcher;
import com.peach.scheduler.provider.SchedulingProvider;
import com.peach.scheduler.provider.SchedulingProviderResolver;
import com.peach.scheduler.service.RocketJobDispatcher;
import com.peach.scheduler.service.SchedulerCronService;
import com.peach.scheduler.service.SchedulerExecutionLifecycleService;
import com.peach.scheduler.service.ISchedulerExecutionService;
import com.peach.scheduler.service.ISchedulerHandlerService;
import com.peach.scheduler.service.SchedulerJobLifecycleService;
import com.peach.scheduler.service.ISchedulerJobService;
import com.peach.scheduler.service.impl.SchedulerExecutionServiceImpl;
import com.peach.scheduler.service.impl.SchedulerHandlerServiceImpl;
import com.peach.scheduler.service.impl.SchedulerJobServiceImpl;
import com.peach.scheduler.service.SchedulerReconciler;
import com.peach.scheduler.service.SchedulerRetryRecoveryService;
import com.peach.scheduler.service.SchedulerTriggerService;
import com.peach.scheduler.statemachine.ExecutionStateMachineFactory;
import com.peach.scheduler.statemachine.JobStateMachineFactory;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 调度服务配置类。
 * <p>调度模块说明。
 * 调度模块说明。
 * 调度模块说明。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Configuration
@EnableScheduling
@Indexed
public class SchedulerServiceConfiguration {

    /**
     * 创建实例。
     *
     * @return 执行结果。
     */
    @Bean
    public JobStateMachineFactory jobStateMachineFactory() {
        return new JobStateMachineFactory();
    }

    /**
     * 创建实例。
     *
     * @return 执行结果。
     */
    @Bean
    public ExecutionStateMachineFactory executionStateMachineFactory() {
        return new ExecutionStateMachineFactory();
    }

    /**
     * 创建实例。
     *
     * @param jobDao job Dao。
     * @param jobVersionDao job Version Dao。
     * @param stateLogDao state Log Dao。
     * @param stateMachineFactory state Machine Factory。
     * @return 执行结果。
     */
    @Bean
    public SchedulerJobLifecycleService schedulerJobLifecycleService(
            SchedulerJobDao jobDao,
            SchedulerJobVersionDao jobVersionDao,
            SchedulerStateLogDao stateLogDao,
            JobStateMachineFactory stateMachineFactory) {
        return new SchedulerJobLifecycleService(jobDao, jobVersionDao, stateLogDao, stateMachineFactory);
    }

    /**
     * 创建实例。
     *
     * @param executionDao execution Dao。
     * @param executionAttemptDao execution Attempt Dao。
     * @param stateLogDao state Log Dao。
     * @param stateMachineFactory state Machine Factory。
     * @return 执行结果。
     */
    @Bean
    public SchedulerExecutionLifecycleService schedulerExecutionLifecycleService(
            SchedulerExecutionDao executionDao,
            SchedulerExecutionAttemptDao executionAttemptDao,
            SchedulerStateLogDao stateLogDao,
            ExecutionStateMachineFactory stateMachineFactory,
            ObjectProvider<SchedulerExecutionLifecycleService> lifecycleServiceProvider) {
        return new SchedulerExecutionLifecycleService(
                executionDao, executionAttemptDao, stateLogDao, stateMachineFactory, lifecycleServiceProvider);
    }

    /**
     * 创建实例。
     *
     * @param outboxPublisher outbox Publisher。
     * @return 执行结果。
     */
    @Bean
    public JobDispatcher schedulerJobDispatcher(MqOutboxPublisher outboxPublisher) {
        return new RocketJobDispatcher(outboxPublisher);
    }

    /**
     * 创建实例。
     *
     * @param jobDao job Dao。
     * @param executionDao execution Dao。
     * @param lifecycleService lifecycle Service。
     * @param dispatcher dispatcher。
     * @param operationLogDao operation Log Dao。
     * @return 执行结果。
     */
    @Bean
    public SchedulerTriggerService schedulerTriggerService(
            SchedulerJobDao jobDao,
            SchedulerExecutionDao executionDao,
            SchedulerExecutionLifecycleService lifecycleService,
            JobDispatcher dispatcher,
            SchedulerOperationLogDao operationLogDao) {
        return new SchedulerTriggerService(
                jobDao, executionDao, lifecycleService, dispatcher, operationLogDao);
    }

    /**
     * 创建实例。
     *
     * @param jobDao job Dao。
     * @param handlerDao handler Dao。
     * @param jobVersionDao job Version Dao。
     * @param lifecycleService lifecycle Service。
     * @param objectMapper object Mapper。
     * @param operationLogDao operation Log Dao。
     * @return 执行结果。
     */
    @Bean
    public ISchedulerJobService schedulerJobService(
            SchedulerJobDao jobDao,
            SchedulerHandlerDao handlerDao,
            SchedulerJobVersionDao jobVersionDao,
            SchedulerJobLifecycleService lifecycleService,
            ObjectMapper objectMapper,
            SchedulerOperationLogDao operationLogDao) {
        return new SchedulerJobServiceImpl(
                jobDao, handlerDao, jobVersionDao, lifecycleService, objectMapper, operationLogDao);
    }

    /**
     * 创建实例。
     *
     * @param executionDao execution Dao。
     * @param jobDao job Dao。
     * @param lifecycleService lifecycle Service。
     * @param triggerService trigger Service。
     * @param operationLogDao operation Log Dao。
     * @return 执行结果。
     */
    @Bean
    public ISchedulerExecutionService schedulerExecutionService(
            SchedulerExecutionDao executionDao,
            SchedulerJobDao jobDao,
            SchedulerExecutionLifecycleService lifecycleService,
            SchedulerTriggerService triggerService,
            SchedulerOperationLogDao operationLogDao) {
        return new SchedulerExecutionServiceImpl(
                executionDao, jobDao, lifecycleService, triggerService, operationLogDao);
    }

    /**
     * 创建实例。
     *
     * @param executionDao execution Dao。
     * @param jobDao job Dao。
     * @param lifecycleService lifecycle Service。
     * @param triggerService trigger Service。
     * @return 执行结果。
     */
    @Bean
    public SchedulerRetryRecoveryService schedulerRetryRecoveryService(
            SchedulerExecutionDao executionDao,
            SchedulerJobDao jobDao,
            SchedulerExecutionLifecycleService lifecycleService,
            SchedulerTriggerService triggerService) {
        return new SchedulerRetryRecoveryService(executionDao, jobDao, lifecycleService, triggerService);
    }

    /**
     * 创建实例。
     *
     * @return 执行结果。
     */
    @Bean
    public SchedulerCronService schedulerCronService() {
        return new SchedulerCronService();
    }

    /**
     * 创建实例。
     *
     * @param handlerDao handler Dao。
     * @return 执行结果。
     */
    @Bean
    public ISchedulerHandlerService schedulerHandlerService(SchedulerHandlerDao handlerDao) {
        return new SchedulerHandlerServiceImpl(handlerDao);
    }

    /**
     * 创建实例。
     *
     * @param jobDao job Dao。
     * @param providers providers。
     * @param providerId provider Id。
     * @return 执行结果。
     */
    @Bean
    public SchedulerReconciler schedulerReconciler(
            SchedulerJobDao jobDao,
            List<SchedulingProvider> providers,
            @Value("${peach.scheduler.provider:quartz}") String providerId) {
        SchedulingProviderResolver resolver = new SchedulingProviderResolver(providers);
        return new SchedulerReconciler(jobDao, resolver.getRequired(providerId));
    }
}
