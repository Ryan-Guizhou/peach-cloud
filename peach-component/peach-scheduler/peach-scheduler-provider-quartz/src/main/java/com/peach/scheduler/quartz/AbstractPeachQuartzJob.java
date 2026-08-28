package com.peach.scheduler.quartz;

import com.peach.scheduler.provider.ScheduleTriggerContext;
import com.peach.scheduler.provider.ScheduleTriggerHandler;
import java.time.Instant;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * AbstractPeachQuartzJob相关类。
 * <p>调度模块说明。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public abstract class AbstractPeachQuartzJob implements Job {
    /**
     * 调度模块说明。
     */
    public static final String CONTEXT_TRIGGER_HANDLER = "peachScheduleTriggerHandler";
    /**
     * 调度模块说明。
     */
    public static final String KEY_JOB_CODE = "jobCode";
    /**
     * 调度模块说明。
     */
    public static final String KEY_PARAMETERS = "parameters";

    /**
     * 创建实例。
     */
    protected AbstractPeachQuartzJob() {
    }

    /**
     * 接口实现。
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
