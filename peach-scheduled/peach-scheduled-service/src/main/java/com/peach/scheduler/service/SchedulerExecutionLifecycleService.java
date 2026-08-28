package com.peach.scheduler.service;

import java.time.ZoneId;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Indexed;

import com.peach.scheduled.common.ExecutionEvent;
import com.peach.scheduled.common.ExecutionState;
import com.peach.scheduler.dao.SchedulerExecutionAttemptDao;
import com.peach.scheduler.dao.ExecutionCompletionCommand;
import com.peach.scheduler.dao.SchedulerExecutionDao;
import com.peach.scheduler.dao.SchedulerStateLogDao;
import com.peach.scheduled.entity.SchedulerExecutionDO;
import com.peach.scheduler.statemachine.ExecutionStateMachineFactory;
import com.peach.scheduler.statemachine.StateMachineTransitionResolver;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.transaction.annotation.Transactional;

/**
 * 调度执行Lifecycle服务类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
public class SchedulerExecutionLifecycleService {
    private static final String ENTITY_TYPE_EXECUTION = "EXECUTION";

    private final SchedulerExecutionDao executionDao;
    private final SchedulerExecutionAttemptDao executionAttemptDao;
    private final SchedulerStateLogDao stateLogDao;
    private final ExecutionStateMachineFactory stateMachineFactory;

    private final ObjectProvider<SchedulerExecutionLifecycleService> self;

    /**
     * 创建实例。
     *
     * @param executionDao execution Dao。
     * @param executionAttemptDao execution Attempt Dao。
     * @param stateLogDao state Log Dao。
     * @param stateMachineFactory state Machine Factory。
     */
    public SchedulerExecutionLifecycleService(
            SchedulerExecutionDao executionDao,
            SchedulerExecutionAttemptDao executionAttemptDao,
            SchedulerStateLogDao stateLogDao,
            ExecutionStateMachineFactory stateMachineFactory,
            ObjectProvider<SchedulerExecutionLifecycleService> self) {
        this.executionDao = executionDao;
        this.executionAttemptDao = executionAttemptDao;
        this.stateLogDao = stateLogDao;
        this.stateMachineFactory = stateMachineFactory;
        this.self = self;
    }

    /**
     * 执行相关状态迁移。
     *
     * @param executionId execution Id。
     */
    @Transactional
    public void queue(String executionId) {
        self.getObject().transition(executionId, ExecutionEvent.QUEUE, null, null, "system");
    }

    /**
     * 调度模块说明。
     *
     * @return 执行结果。
     * @param executionId 执行实例 ID
     * @param executorInstance 执行器实例标识
     * @param leaseSeconds 执行租约时长，单位秒
     */
    @Transactional
    public boolean claim(String executionId, String executorInstance, long leaseSeconds) {
        SchedulerExecutionDO execution = required(executionId);
        if (execution.getState() != ExecutionState.QUEUED) return false;
        ExecutionState target = StateMachineTransitionResolver.transit(
                stateMachineFactory.create(execution.getState()), ExecutionEvent.CLAIM);
        LocalDateTime start = LocalDateTime.now(ZoneId.systemDefault());
        int updated = executionDao.claim(
                executionId, executorInstance, start.plusSeconds(leaseSeconds), execution.getVersion());
        if (updated != 1) return false;
        executionAttemptDao.insertStart(executionId, execution.getAttempt(), executorInstance, start);
        stateLogDao.insert(ENTITY_TYPE_EXECUTION, executionId, execution.getState().name(),
                ExecutionEvent.CLAIM.name(), target.name(), executorInstance, null);
        return true;
    }

    /**
     * 完成相关处理。
     * @param executionId 执行实例 ID
     * @param event 状态机事件
     * @param executorInstance 执行器实例标识
     * @param errorType 错误类型
     * @param errorMessage 已脱敏错误摘要
     */
    @Transactional
    public void complete(String executionId, ExecutionEvent event, String executorInstance,
                         String errorType, String errorMessage) {
        SchedulerExecutionDO execution = required(executionId);
        ExecutionState target = StateMachineTransitionResolver.transit(
                stateMachineFactory.create(execution.getState()), event);
        LocalDateTime finish = LocalDateTime.now(ZoneId.systemDefault());
        long durationMs = execution.getStartTime() == null
                ? 0L
                : ChronoUnit.MILLIS.between(
                        execution.getStartTime().atZone(ZoneId.systemDefault()).toInstant(),
                        finish.atZone(ZoneId.systemDefault()).toInstant());
        int updated = executionDao.complete(new ExecutionCompletionCommand(
                executionId, execution.getState().name(), target.name(), execution.getVersion(),
                executorInstance, finish, durationMs, errorType, errorMessage));
        if (updated != 1) throw new IllegalStateException("Execution completion rejected by optimistic lock");
        executionAttemptDao.complete(executionId, execution.getAttempt(), target.name(), finish, durationMs,
                errorType, errorMessage);
        stateLogDao.insert(ENTITY_TYPE_EXECUTION, executionId, execution.getState().name(), event.name(),
                target.name(), executorInstance, null);
    }

    /**
     * 执行相关状态迁移。
     * @param executionId 执行实例 ID
     * @param executorInstance 执行器实例标识
     * @param nextRetryTime 下一次允许重试时间
     * @param errorType 错误类型
     * @param errorMessage 已脱敏错误摘要
     */
    @Transactional
    public void scheduleRetry(String executionId, String executorInstance, LocalDateTime nextRetryTime,
                              String errorType, String errorMessage) {
        SchedulerExecutionDO execution = required(executionId);
        ExecutionState target = StateMachineTransitionResolver.transit(
                stateMachineFactory.create(execution.getState()), ExecutionEvent.FAIL);
        int updated = executionDao.scheduleRetry(executionId, execution.getVersion(), nextRetryTime,
                errorType, errorMessage);
        if (updated != 1) throw new IllegalStateException("Execution retry scheduling rejected by optimistic lock");
        LocalDateTime finish = LocalDateTime.now(ZoneId.systemDefault());
        long durationMs = execution.getStartTime() == null
                ? 0L
                : ChronoUnit.MILLIS.between(
                        execution.getStartTime().atZone(ZoneId.systemDefault()).toInstant(),
                        finish.atZone(ZoneId.systemDefault()).toInstant());
        executionAttemptDao.complete(executionId, execution.getAttempt(), target.name(), finish, durationMs,
                errorType, errorMessage);
        stateLogDao.insert(ENTITY_TYPE_EXECUTION, executionId, execution.getState().name(),
                ExecutionEvent.FAIL.name(), target.name(), executorInstance, null);
    }

    /**
     * 重新入队相关执行。
     * @param executionId 执行实例 ID
     * @return 成功重新入队返回 {@code true}
     */
    @Transactional
    public boolean requeueRetry(String executionId) {
        SchedulerExecutionDO execution = required(executionId);
        if (execution.getState() != ExecutionState.RETRY_WAIT) return false;
        ExecutionState target = StateMachineTransitionResolver.transit(
                stateMachineFactory.create(execution.getState()), ExecutionEvent.RETRY);
        int updated = executionDao.requeueRetry(executionId, execution.getVersion());
        if (updated != 1) return false;
        stateLogDao.insert(ENTITY_TYPE_EXECUTION, executionId, execution.getState().name(),
                ExecutionEvent.RETRY.name(), target.name(), "system", null);
        return true;
    }

    /**
     * 调度模块说明。
     * @param executionId 执行实例 ID
     * @param event 状态机事件
     * @param errorType 错误类型
     * @param errorMessage 已脱敏错误摘要
     * @param operator 操作人或系统实例标识
     */
    @Transactional
    public void transition(String executionId, ExecutionEvent event, String errorType,
                           String errorMessage, String operator) {
        SchedulerExecutionDO execution = required(executionId);
        ExecutionState target = StateMachineTransitionResolver.transit(
                stateMachineFactory.create(execution.getState()), event);
        int updated = executionDao.updateState(executionId, execution.getState().name(), target.name(),
                execution.getVersion(), errorType, errorMessage);
        if (updated != 1) throw new IllegalStateException("Concurrent execution update detected");
        stateLogDao.insert(ENTITY_TYPE_EXECUTION, executionId, execution.getState().name(), event.name(),
                target.name(), operator, null);
    }

    private SchedulerExecutionDO required(String executionId) {
        SchedulerExecutionDO execution = executionDao.selectById(executionId);
        if (execution == null) throw new IllegalArgumentException("Execution not found: " + executionId);
        return execution;
    }
}
