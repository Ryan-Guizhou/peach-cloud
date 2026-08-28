package com.peach.scheduler.service;

import org.springframework.stereotype.Indexed;

import com.peach.scheduled.common.JobEvent;
import com.peach.scheduled.common.JobState;
import com.peach.scheduler.dao.SchedulerJobDao;
import com.peach.scheduler.dao.SchedulerJobVersionDao;
import com.peach.scheduler.dao.SchedulerStateLogDao;
import com.peach.scheduled.entity.SchedulerJobDO;
import com.peach.scheduler.statemachine.JobStateMachineFactory;
import com.peach.scheduler.statemachine.StateMachineTransitionResolver;
import org.springframework.transaction.annotation.Transactional;

/**
 * 调度任务Lifecycle服务类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
public class SchedulerJobLifecycleService {
    private final SchedulerJobDao jobDao;
    private final SchedulerJobVersionDao jobVersionDao;
    private final SchedulerStateLogDao stateLogDao;
    private final JobStateMachineFactory stateMachineFactory;

    /**
     * 创建实例。
     * @param jobDao 任务定义数据访问对象
     * @param jobVersionDao 任务定义版本快照数据访问对象
     * @param stateLogDao 状态迁移审计日志数据访问对象
     * @param stateMachineFactory 任务状态机工厂
     */
    public SchedulerJobLifecycleService(SchedulerJobDao jobDao,
                                        SchedulerJobVersionDao jobVersionDao,
                                        SchedulerStateLogDao stateLogDao,
                                        JobStateMachineFactory stateMachineFactory) {
        this.jobDao = jobDao;
        this.jobVersionDao = jobVersionDao;
        this.stateLogDao = stateLogDao;
        this.stateMachineFactory = stateMachineFactory;
    }

    /**
     * 执行相关状态处理。
     *
     * @param jobId job Id。
     * @param event event。
     * @param operatorId operator Id。
     * @return 执行结果。
     */
    @Transactional
    public SchedulerJobDO transition(String jobId, JobEvent event, String operatorId) {
        SchedulerJobDO job = required(jobId);
        JobState fromState = job.getState();
        JobState toState = StateMachineTransitionResolver.transit(
                stateMachineFactory.create(fromState), event);
        int updated = jobDao.updateState(jobId, fromState.name(), toState.name(),
                job.getVersion(), operatorId);
        if (updated != 1) throw new IllegalStateException("Concurrent scheduler job update detected");
        SchedulerJobDO refreshed = required(jobId);
        jobVersionDao.insertSnapshot(refreshed);
        stateLogDao.insert("JOB", String.valueOf(jobId), fromState.name(), event.name(),
                toState.name(), operatorId, null);
        return refreshed;
    }

    private SchedulerJobDO required(String jobId) {
        SchedulerJobDO job = jobDao.selectById(jobId);
        if (job == null) throw new IllegalArgumentException("Scheduler job not found: " + jobId);
        return job;
    }
}
