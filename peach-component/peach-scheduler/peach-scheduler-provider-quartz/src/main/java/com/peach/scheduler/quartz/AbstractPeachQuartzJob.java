package com.peach.scheduler.quartz;

import com.peach.scheduler.provider.ScheduleTriggerContext;
import com.peach.scheduler.provider.ScheduleTriggerHandler;
import java.time.Instant;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Quartz Job 基类，负责把 Quartz 触发转换为 Peach 调度触发上下文。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public abstract class AbstractPeachQuartzJob implements Job {
    /**
     * Quartz SchedulerContext 中保存触发处理器的 key。
     */
    public static final String CONTEXT_TRIGGER_HANDLER = "peachScheduleTriggerHandler";
    /**
     * Quartz JobDataMap 中保存任务编码的 key。
     */
    public static final String KEY_JOB_CODE = "jobCode";
    /**
     * Quartz JobDataMap 中保存任务参数的 key。
     */
    public static final String KEY_PARAMETERS = "parameters";

    /**
     * 创建 Quartz Job 基类实例。
     */
    protected AbstractPeachQuartzJob() {
    }

    /**
     * 处理 Quartz 触发并委托给 Peach 调度触发处理器。
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            Object value = context.getScheduler().getContext().get(CONTEXT_TRIGGER_HANDLER);
            if (!(value instanceof ScheduleTriggerHandler handler)) {
                throw new IllegalStateException("ScheduleTriggerHandler is not registered in Quartz SchedulerContext");
            }
            ScheduleTriggerContext trigger = new ScheduleTriggerContext(
                    context.getMergedJobDataMap().getString(KEY_JOB_CODE),
                    context.getScheduledFireTime() == null
                            ? Instant.now() : context.getScheduledFireTime().toInstant(),
                    context.getMergedJobDataMap().getString(KEY_PARAMETERS),
                    QuartzSchedulingProvider.PROVIDER_ID);
            handler.onTrigger(trigger);
        } catch (Exception ex) {
            throw new JobExecutionException("Failed to process Peach scheduler Quartz trigger", ex, false);
        }
    }
}
