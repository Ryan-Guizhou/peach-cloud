package com.peach.scheduler.service;

import org.springframework.stereotype.Indexed;

import com.peach.scheduled.common.JobState;
import com.peach.scheduler.dao.SchedulerJobDao;
import com.peach.scheduled.entity.SchedulerJobDO;
import com.peach.scheduler.model.ConcurrencyPolicy;
import com.peach.scheduler.model.JobDefinition;
import com.peach.scheduler.model.MisfirePolicy;
import com.peach.scheduler.model.ScheduleType;
import com.peach.scheduler.provider.SchedulingProvider;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 调度模块相关说明。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
public class SchedulerReconciler {
    private static final Logger log = LoggerFactory.getLogger(SchedulerReconciler.class);
    private final SchedulerJobDao jobDao;
    private final SchedulingProvider provider;

    /**
     * 创建相关对象。
     * @param jobDao 任务定义数据访问对象
     * @param provider 当前启用的调度 Provider
     */
    public SchedulerReconciler(SchedulerJobDao jobDao, SchedulingProvider provider) {
        this.jobDao = jobDao;
        this.provider = provider;
    }

    /**
     * 调度模块相关说明。
     */
    @Scheduled(fixedDelayString = "${peach.scheduler.service.reconcile-delay-ms:5000}")
    public void reconcile() {
        List<SchedulerJobDO> jobs = jobDao.selectPendingSync(100);
        for (SchedulerJobDO job : jobs) {
            try {
                apply(job);
                jobDao.markSyncSuccess(job.getId(), job.getScheduleVersion());
            } catch (RuntimeException ex) {
                String message = sanitize(ex.getMessage(), ex.getClass().getSimpleName());
                jobDao.markSyncFailure(job.getId(), job.getScheduleVersion(), message);
                log.error("Scheduler provider reconciliation failed, jobCode={}, scheduleVersion={}, errorType={}",
                        job.getJobCode(), job.getScheduleVersion(), ex.getClass().getName(), ex);
            }
        }
    }

    private void apply(SchedulerJobDO job) {
        if (job.getState() == JobState.DELETED || job.getState() == JobState.DISABLED) {
            if (provider.exists(job.getJobCode())) provider.delete(job.getJobCode());
            return;
        }
        JobDefinition definition = toDefinition(job);
        if (provider.exists(job.getJobCode())) provider.reschedule(definition);
        else provider.schedule(definition);
        if (job.getState() == JobState.PAUSED) provider.pause(job.getJobCode());
        else provider.resume(job.getJobCode());
    }

    private JobDefinition toDefinition(SchedulerJobDO job) {
        JobDefinition definition = new JobDefinition();
        definition.setJobCode(job.getJobCode());
        definition.setApplicationName(job.getApplicationName());
        definition.setHandlerName(job.getHandlerName());
        definition.setScheduleType(ScheduleType.valueOf(job.getScheduleType()));
        definition.setCronExpression(job.getCronExpression());
        definition.setIntervalSeconds(job.getIntervalSeconds() == null ? 0L : job.getIntervalSeconds());
        definition.setStartAt(job.getStartAt() == null ? null : job.getStartAt().toInstant(ZoneOffset.UTC));
        definition.setTimezone(job.getTimeZone());
        definition.setMisfirePolicy(MisfirePolicy.valueOf(job.getMisfirePolicy()));
        definition.setConcurrencyPolicy(ConcurrencyPolicy.valueOf(job.getConcurrencyPolicy()));
        definition.setParameters(job.getParametersJson());
        definition.setEnabled(job.getState() == JobState.ENABLED);
        return definition;
    }

    private String sanitize(String message, String fallback) {
        String value = message == null ? fallback : message.replace('\r', ' ').replace('\n', ' ');
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
