package com.peach.scheduler.service;

import java.time.ZoneId;

import org.springframework.stereotype.Indexed;

import com.peach.scheduled.common.ExecutionEvent;
import com.peach.scheduler.dao.SchedulerExecutionDao;
import com.peach.scheduler.dao.SchedulerJobDao;
import com.peach.scheduled.entity.SchedulerExecutionDO;
import com.peach.scheduled.entity.SchedulerJobDO;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 调度重试Recovery服务类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
public class SchedulerRetryRecoveryService {
    private static final String ERROR_MESSAGE_LEASE_EXPIRED = "Executor lease expired";

    private static final String ERROR_TYPE_LEASE_EXPIRED = "LEASE_EXPIRED";

    private static final Logger log = LoggerFactory.getLogger(SchedulerRetryRecoveryService.class);
    private final SchedulerExecutionDao executionDao;
    private final SchedulerJobDao jobDao;
    private final SchedulerExecutionLifecycleService lifecycleService;
    private final SchedulerTriggerService triggerService;

    /**
     * 创建实例。
     * @param executionDao 执行实例数据访问对象
     * @param jobDao 任务定义数据访问对象
     * @param lifecycleService 生命周期服务
     * @param triggerService 任务触发服务
     */
    public SchedulerRetryRecoveryService(SchedulerExecutionDao executionDao,
                                         SchedulerJobDao jobDao,
                                         SchedulerExecutionLifecycleService lifecycleService,
                                         SchedulerTriggerService triggerService) {
        this.executionDao = executionDao;
        this.jobDao = jobDao;
        this.lifecycleService = lifecycleService;
        this.triggerService = triggerService;
    }

    /**
     * 处理相关数据。
     */
    @Scheduled(fixedDelayString = "${peach.scheduler.service.recovery-delay-ms:5000}")
    public void run() {
        recoverExpired(100);
        dispatchDueRetries(100);
        dispatchDeferred(100);
    }

    private void dispatchDeferred(int limit) {
        List<SchedulerExecutionDO> deferred = executionDao.selectDeferredCreated(limit);
        for (SchedulerExecutionDO execution : deferred) {
            try {
                triggerService.dispatchDeferred(execution.getExecutionId());
            } catch (RuntimeException ex) {
                log.error("Scheduler deferred dispatch failed, executionId={}, errorType={}",
                        execution.getExecutionId(), ex.getClass().getName(), ex);
            }
        }
    }

    private void recoverExpired(int limit) {
        List<SchedulerExecutionDO> expired = executionDao.selectExpiredRunning(LocalDateTime.now(ZoneId.systemDefault()), limit);
        for (SchedulerExecutionDO execution : expired) {
            try {
                SchedulerJobDO job = jobDao.selectById(execution.getJobId());
                int maxAttempts = job == null || job.getMaxAttempts() == null ? 1 : Math.max(1, job.getMaxAttempts());
                int retryIntervalSeconds = job == null || job.getRetryIntervalSeconds() == null
                        ? 60 : Math.max(1, job.getRetryIntervalSeconds());
                if (execution.getAttempt() >= maxAttempts) {
                    lifecycleService.scheduleRetry(execution.getExecutionId(), execution.getExecutorInstance(),
                            LocalDateTime.now(ZoneId.systemDefault()), ERROR_TYPE_LEASE_EXPIRED, ERROR_MESSAGE_LEASE_EXPIRED);
                    lifecycleService.transition(execution.getExecutionId(), ExecutionEvent.EXHAUST,
                            ERROR_TYPE_LEASE_EXPIRED, ERROR_MESSAGE_LEASE_EXPIRED, "system");
                } else {
                    int delay = retryIntervalSeconds;
                    lifecycleService.scheduleRetry(execution.getExecutionId(), execution.getExecutorInstance(),
                            LocalDateTime.now(ZoneId.systemDefault()).plusSeconds(delay), ERROR_TYPE_LEASE_EXPIRED, ERROR_MESSAGE_LEASE_EXPIRED);
                }
            } catch (RuntimeException ex) {
                log.error("Scheduler lease recovery failed, executionId={}, errorType={}",
                        execution.getExecutionId(), ex.getClass().getName(), ex);
            }
        }
    }

    private void dispatchDueRetries(int limit) {
        List<SchedulerExecutionDO> due = executionDao.selectDueRetries(LocalDateTime.now(ZoneId.systemDefault()), limit);
        for (SchedulerExecutionDO execution : due) {
            try {
                SchedulerJobDO job = jobDao.selectById(execution.getJobId());
                if (job == null) {
                    lifecycleService.transition(execution.getExecutionId(), ExecutionEvent.EXHAUST,
                            "JOB_MISSING", "Scheduler job no longer exists", "system");
                    continue;
                }
                triggerService.dispatchRetry(execution.getExecutionId());
            } catch (RuntimeException ex) {
                log.error("Scheduler retry dispatch failed, executionId={}, errorType={}",
                        execution.getExecutionId(), ex.getClass().getName(), ex);
            }
        }
    }
}
