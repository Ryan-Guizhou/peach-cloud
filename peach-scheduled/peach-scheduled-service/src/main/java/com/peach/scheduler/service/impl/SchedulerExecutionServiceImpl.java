package com.peach.scheduler.service.impl;

import java.time.ZoneId;

import org.springframework.stereotype.Indexed;

import com.peach.scheduler.service.ISchedulerExecutionService;
import com.peach.scheduler.service.SchedulerExecutionLifecycleService;
import com.peach.scheduler.service.SchedulerTriggerService;
import com.peach.scheduled.common.ExecutionEvent;
import com.peach.scheduled.common.ExecutionState;
import com.peach.scheduler.dao.SchedulerExecutionDao;
import com.peach.scheduler.dao.SchedulerJobDao;
import com.peach.scheduler.dao.SchedulerOperationLogDao;
import com.peach.scheduled.entity.SchedulerExecutionDO;
import com.peach.scheduled.entity.SchedulerJobDO;
import com.peach.scheduler.model.ExecutionResultStatus;
import com.peach.scheduled.qo.SchedulerExecutionQO;
import com.peach.scheduled.vo.SchedulerExecutionVO;
import org.springframework.beans.BeanUtils;
import com.peach.scheduler.transport.JobExecutionResultEvent;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

/**
 * 调度执行实例服务实现。
 *
 * <p>负责执行租约抢占、结果收敛、失败重试、人工重试、人工取消和 DO 到 VO 的边界转换。
 * 所有状态变更委托给执行生命周期服务，避免绕过状态机和乐观锁。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
public class SchedulerExecutionServiceImpl implements ISchedulerExecutionService {

    private static final int MAX_REASON_LENGTH = 500;

    private final SchedulerExecutionDao executionDao;
    private final SchedulerJobDao jobDao;
    private final SchedulerExecutionLifecycleService lifecycleService;
    private final SchedulerTriggerService triggerService;
    private final SchedulerOperationLogDao operationLogDao;

    /**
     * 创建调度执行实例服务。
     *
     * @param executionDao 执行实例数据访问对象
     * @param jobDao 任务定义数据访问对象
     * @param lifecycleService 执行生命周期服务
     * @param triggerService 执行触发服务
     * @param operationLogDao 人工操作审计数据访问对象
     */
    public SchedulerExecutionServiceImpl(SchedulerExecutionDao executionDao,
                                     SchedulerJobDao jobDao,
                                     SchedulerExecutionLifecycleService lifecycleService,
                                     SchedulerTriggerService triggerService,
                                     SchedulerOperationLogDao operationLogDao) {
        this.executionDao = executionDao;
        this.jobDao = jobDao;
        this.lifecycleService = lifecycleService;
        this.triggerService = triggerService;
        this.operationLogDao = operationLogDao;
    }

    /**
     * 尝试执行相关操作。
     *
     * @param executionId 参数说明
     * @param executorInstance 参数说明
     * @return 返回结果
     */
    @Override
    public boolean claim(String executionId, String executorInstance) {
        SchedulerExecutionDO execution = required(executionId);
        SchedulerJobDO job = jobDao.selectById(execution.getJobId());
        long timeoutMs = job == null || job.getTimeoutMs() == null ? 1800000L : job.getTimeoutMs();
        long leaseSeconds = Math.clamp(timeoutMs / 1000L + 120L, 60L, 86400L);
        return lifecycleService.claim(executionId, executorInstance, leaseSeconds);
    }

    /**
     * 处理相关数据。
     *
     * @param event 参数说明
     */
    @Override
    public void processResult(JobExecutionResultEvent event) {
        SchedulerExecutionDO execution = required(event.getExecutionId());
        if (execution.getState() != ExecutionState.RUNNING) {
            if (isTerminal(execution.getState()) || execution.getState() == ExecutionState.RETRY_WAIT) {
                return;
            }
            throw new IllegalStateException("Execution is not running: " + event.getExecutionId());
        }
        if (event.getStatus() == ExecutionResultStatus.SUCCEEDED) {
            lifecycleService.complete(event.getExecutionId(), ExecutionEvent.SUCCESS, event.getExecutorInstance(),
                    event.getResultCode(), null);
            return;
        }
        if (event.getStatus() == ExecutionResultStatus.TIMED_OUT) {
            lifecycleService.complete(event.getExecutionId(), ExecutionEvent.TIMEOUT, event.getExecutorInstance(),
                    event.getResultCode(), event.getErrorMessage());
            return;
        }
        SchedulerJobDO job = jobDao.selectById(execution.getJobId());
        int maxAttempts = job == null || job.getMaxAttempts() == null ? 1 : Math.max(1, job.getMaxAttempts());
        int retryIntervalSeconds = job == null || job.getRetryIntervalSeconds() == null
                ? 60 : Math.max(1, job.getRetryIntervalSeconds());
        if (execution.getAttempt() < maxAttempts) {
            lifecycleService.scheduleRetry(event.getExecutionId(), event.getExecutorInstance(),
                    LocalDateTime.now(ZoneId.systemDefault()).plusSeconds(retryIntervalSeconds), event.getResultCode(), event.getErrorMessage());
        } else {
            lifecycleService.scheduleRetry(event.getExecutionId(), event.getExecutorInstance(), LocalDateTime.now(ZoneId.systemDefault()),
                    event.getResultCode(), event.getErrorMessage());
            lifecycleService.transition(event.getExecutionId(), ExecutionEvent.EXHAUST,
                    event.getResultCode(), event.getErrorMessage(), "system");
        }
    }

    /**
     * 调度模块相关说明。
     *
     * <p>调度模块相关说明。
     * 调度模块相关说明。
     * 调度模块相关说明。</p>
     *
     * @param executionId 参数说明
     * @param operatorId 参数说明
     * @param reason 参数说明
     * @return 返回结果
     * @throws IllegalStateException 异常说明
     */
    @Transactional
    @Override
    public boolean retry(String executionId, String operatorId, String reason) {
        SchedulerExecutionDO execution = required(executionId);
        if (execution.getState() != ExecutionState.RETRY_WAIT) {
            throw new IllegalStateException("Only RETRY_WAIT executions can be manually retried");
        }
        String auditedReason = requireReason(reason);
        boolean dispatched = triggerService.dispatchRetry(executionId);
        if (!dispatched) {
            throw new IllegalStateException("Execution retry was rejected by concurrent state change");
        }
        operationLogDao.insertSuccess("RETRY", "EXECUTION", executionId, operatorId, auditedReason);
        return true;
    }

    /**
     * 调度模块相关说明。
     *
     * <p>调度模块相关说明。
     * 调度模块相关说明。
     * 调度模块相关说明。</p>
     *
     * @param executionId 参数说明
     * @param operatorId 参数说明
     * @param reason 参数说明
     * @return 返回结果
     * @throws IllegalStateException 异常说明
     */
    @Transactional
    @Override
    public SchedulerExecutionVO cancel(String executionId, String operatorId, String reason) {
        SchedulerExecutionDO execution = required(executionId);
        if (execution.getState() != ExecutionState.CREATED
                && execution.getState() != ExecutionState.QUEUED
                && execution.getState() != ExecutionState.RETRY_WAIT) {
            throw new IllegalStateException("Only CREATED, QUEUED or RETRY_WAIT executions can be cancelled");
        }
        String auditedReason = requireReason(reason);
        lifecycleService.transition(executionId, ExecutionEvent.CANCEL,
                "OPERATOR_CANCELLED", null, operatorId);
        operationLogDao.insertSuccess("CANCEL", "EXECUTION", executionId, operatorId, auditedReason);
        return toVO(required(executionId));
    }

    /**
     * 获取相关数据。
     *
     * @param query 参数说明
     * @return 返回结果
     */
    @Override
    public List<SchedulerExecutionVO> list(SchedulerExecutionQO query) {
        return toVOList(executionDao.selectPage(query));
    }

    /**
     * 获取相关数据。
     *
     * @param executionId 参数说明
     * @return 返回结果
     */
    @Override
    public SchedulerExecutionVO get(String executionId) {
        return toVO(required(executionId));
    }

    private SchedulerExecutionVO toVO(SchedulerExecutionDO execution) {
        SchedulerExecutionVO vo = new SchedulerExecutionVO();
        BeanUtils.copyProperties(execution, vo);
        return vo;
    }

    private List<SchedulerExecutionVO> toVOList(List<SchedulerExecutionDO> executions) {
        java.util.ArrayList<SchedulerExecutionVO> result = new java.util.ArrayList<SchedulerExecutionVO>(executions.size());
        for (SchedulerExecutionDO execution : executions) {
            result.add(toVO(execution));
        }
        return result;
    }

    private boolean isTerminal(ExecutionState state) {
        return state == ExecutionState.SUCCEEDED || state == ExecutionState.TIMED_OUT
                || state == ExecutionState.DEAD || state == ExecutionState.CANCELLED
                || state == ExecutionState.SKIPPED;
    }

    private String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Operation reason is required");
        }
        String normalized = reason.trim();
        if (normalized.length() > MAX_REASON_LENGTH) {
            throw new IllegalArgumentException("Operation reason exceeds 500 characters");
        }
        return normalized;
    }

    private SchedulerExecutionDO required(String executionId) {
        SchedulerExecutionDO execution = executionDao.selectById(executionId);
        if (execution == null) {
            throw new IllegalArgumentException("Execution not found: " + executionId);
        }
        return execution;
    }
}
