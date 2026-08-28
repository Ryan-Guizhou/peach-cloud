package com.peach.scheduler.service;

import org.springframework.stereotype.Indexed;

import com.peach.common.IDGeneratorUtil;
import com.peach.scheduled.common.ExecutionEvent;
import com.peach.scheduled.common.ExecutionState;
import com.peach.scheduled.common.JobState;
import com.peach.scheduled.common.TriggerType;
import com.peach.scheduler.dao.SchedulerExecutionDao;
import com.peach.scheduler.dao.SchedulerJobDao;
import com.peach.scheduler.dao.SchedulerOperationLogDao;
import com.peach.scheduler.dispatch.JobDispatcher;
import com.peach.scheduled.entity.SchedulerExecutionDO;
import com.peach.scheduled.entity.SchedulerJobDO;
import com.peach.scheduler.model.ConcurrencyPolicy;
import com.peach.scheduler.provider.ScheduleTriggerContext;
import com.peach.scheduler.provider.ScheduleTriggerHandler;
import com.peach.scheduler.transport.JobExecutionCommand;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * 调度Trigger服务类。
 * <p>调度模块说明。
 * 调度模块说明。
 * 调度模块说明。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
public class SchedulerTriggerService implements ScheduleTriggerHandler {
    private static final String SYSTEM_OPERATOR = "system";


    private static final Logger log = LoggerFactory.getLogger(SchedulerTriggerService.class);

    private final SchedulerJobDao jobDao;
    private final SchedulerExecutionDao executionDao;
    private final SchedulerExecutionLifecycleService lifecycleService;
    private final JobDispatcher dispatcher;
    private final SchedulerOperationLogDao operationLogDao;

    /**
     * 创建实例。
     *
     * @param jobDao job Dao。
     * @param executionDao execution Dao。
     * @param lifecycleService lifecycle Service。
     * @param dispatcher dispatcher。
     * @param operationLogDao 操作审计日志数据访问对象
     */
    public SchedulerTriggerService(SchedulerJobDao jobDao,
                                   SchedulerExecutionDao executionDao,
                                   SchedulerExecutionLifecycleService lifecycleService,
                                   JobDispatcher dispatcher,
                                   SchedulerOperationLogDao operationLogDao) {
        this.jobDao = jobDao;
        this.executionDao = executionDao;
        this.lifecycleService = lifecycleService;
        this.dispatcher = dispatcher;
        this.operationLogDao = operationLogDao;
    }

    /**
     * 接口实现。
     */
    @Override
    @Transactional
    public void onTrigger(ScheduleTriggerContext context) {
        SchedulerJobDO job = jobDao.selectByCode(context.jobCode());
        if (job == null || job.getState() != JobState.ENABLED) {
            log.info("Scheduler trigger ignored because job is not enabled, jobCode={}", context.jobCode());
            return;
        }
        createOccurrence(job, TriggerType.SCHEDULED, context.scheduledTime(),
                job.getId() + ":" + context.scheduledTime().toEpochMilli());
    }

    /**
     * 创建实例。
     *
     * @param jobId job Id。
     * @param operatorId operator Id。
     * @return 执行结果。
     */
    @Transactional
    public String triggerManual(String jobId, String operatorId) {
        SchedulerJobDO job = jobDao.selectById(jobId);
        if (job == null || job.getState() == JobState.DELETED) {
            throw new IllegalArgumentException("Scheduler job not found: " + jobId);
        }
        String occurrenceKey = "manual:" + jobId + ":" + IDGeneratorUtil.generateUuid();
        String executionId = createOccurrence(job, TriggerType.MANUAL, Instant.now(), occurrenceKey);
        if (executionId != null) {
            operationLogDao.insertSuccess("RUN", "JOB", String.valueOf(jobId), operatorId, null);
        }
        return executionId;
    }

    /**
     * 调度模块说明。
     *
     * @param executionId execution Id。
     * @return 执行结果。
     */
    @Transactional
    public boolean dispatchRetry(String executionId) {
        SchedulerExecutionDO execution = executionDao.selectById(executionId);
        if (execution == null || execution.getState() != ExecutionState.RETRY_WAIT) {
            return false;
        }
        SchedulerJobDO job = jobDao.selectByIdForUpdate(execution.getJobId());
        if (job == null || job.getState() == JobState.DELETED) {
            lifecycleService.transition(executionId, ExecutionEvent.EXHAUST,
                    "JOB_UNAVAILABLE", "Scheduler job is no longer available", SYSTEM_OPERATOR);
            return false;
        }
        if (!lifecycleService.requeueRetry(executionId)) {
            return false;
        }
        SchedulerExecutionDO refreshed = executionDao.selectById(executionId);
        dispatcher.dispatch(command(job, refreshed));
        return true;
    }

    /**
     * 尝试执行相关操作。
     *
     * <p>调度模块说明。</p>
     *
     * @param executionId execution Id。
     * @return 执行结果。
     */
    @Transactional
    public boolean dispatchDeferred(String executionId) {
        SchedulerExecutionDO execution = executionDao.selectById(executionId);
        if (execution == null || execution.getState() != ExecutionState.CREATED) {
            return false;
        }
        SchedulerJobDO job = jobDao.selectByIdForUpdate(execution.getJobId());
        if (job == null || job.getState() == JobState.DELETED) {
            lifecycleService.transition(executionId, ExecutionEvent.CANCEL,
                    "JOB_UNAVAILABLE", "Scheduler job is no longer available", SYSTEM_OPERATOR);
            return false;
        }
        if (executionDao.countActiveByJobId(job.getId()) > 0) {
            return false;
        }
        dispatcher.dispatch(command(job, execution));
        lifecycleService.queue(executionId);
        return true;
    }

    private String createOccurrence(SchedulerJobDO sourceJob, TriggerType triggerType,
                                    Instant scheduledTime, String occurrenceKey) {
        SchedulerJobDO job = jobDao.selectByIdForUpdate(sourceJob.getId());
        if (job == null || job.getState() == JobState.DELETED) {
            return null;
        }

        SchedulerExecutionDO execution = new SchedulerExecutionDO();
        execution.setExecutionId(IDGeneratorUtil.generateUuid());
        execution.setJobId(job.getId());
        execution.setJobCode(job.getJobCode());
        execution.setOccurrenceKey(occurrenceKey);
        execution.setTriggerType(triggerType.name());
        execution.setScheduledTime(LocalDateTime.ofInstant(scheduledTime, ZoneOffset.UTC));
        execution.setState(ExecutionState.CREATED);
        execution.setAttempt(1);
        execution.setVersion(0L);
        execution.setTraceId(IDGeneratorUtil.generateUuid());

        if (executionDao.insertIgnore(execution) != 1) {
            log.info("Duplicate scheduler occurrence ignored, jobCode={}, occurrenceKey={}",
                    job.getJobCode(), occurrenceKey);
            return null;
        }

        ConcurrencyPolicy policy = concurrencyPolicy(job);
        int activeCount = executionDao.countActiveByJobId(job.getId());
        if (activeCount > 0 && policy == ConcurrencyPolicy.SKIP_IF_RUNNING) {
            lifecycleService.transition(execution.getExecutionId(), ExecutionEvent.SKIP,
                    "CONCURRENCY_POLICY", "Skipped because another occurrence is active", SYSTEM_OPERATOR);
            log.info("Scheduler occurrence skipped by concurrency policy, executionId={}, jobCode={}",
                    execution.getExecutionId(), job.getJobCode());
            return execution.getExecutionId();
        }
        if (activeCount > 0 && policy == ConcurrencyPolicy.DISALLOW) {
            log.info("Scheduler occurrence deferred by concurrency policy, executionId={}, jobCode={}",
                    execution.getExecutionId(), job.getJobCode());
            return execution.getExecutionId();
        }

        dispatcher.dispatch(command(job, execution));
        lifecycleService.queue(execution.getExecutionId());
        return execution.getExecutionId();
    }

    private ConcurrencyPolicy concurrencyPolicy(SchedulerJobDO job) {
        try {
            return ConcurrencyPolicy.valueOf(job.getConcurrencyPolicy());
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Invalid persisted concurrency policy for job " + job.getJobCode(), ex);
        }
    }

    private JobExecutionCommand command(SchedulerJobDO job, SchedulerExecutionDO execution) {
        return new JobExecutionCommand(
                execution.getExecutionId(),
                job.getJobCode(),
                job.getApplicationName(),
                job.getHandlerName(),
                job.getParametersJson(),
                job.getTimeoutMs() == null ? 0L : job.getTimeoutMs(),
                execution.getAttempt(),
                execution.getTraceId());
    }
}
