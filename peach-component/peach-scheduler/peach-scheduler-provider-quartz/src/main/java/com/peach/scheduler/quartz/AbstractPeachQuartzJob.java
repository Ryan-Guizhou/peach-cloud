package com.peach.scheduler.quartz;

import com.peach.scheduler.provider.ScheduleTriggerContext;
import com.peach.scheduler.provider.ScheduleTriggerHandler;
import java.time.Instant;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * 调度模块相关说明。
 *
 * <p>调度模块相关说明。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public abstract class AbstractPeachQuartzJob implements Job {
    /**
     * 调度模块相关说明。
     */
    public static final String CONTEXT_TRIGGER_HANDLER = "peachScheduleTriggerHandler";
    /**
     * 调度模块相关说明。
     */
    public static final String KEY_JOB_CODE = "jobCode";
    /**
     * 调度模块相关说明。
     */
    public static final String KEY_PARAMETERS = "parameters";

    /**
     * 创建相关对象。
     */
    protected AbstractPeachQuartzJob() {
    }

    /**
     * 继承接口定义。
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            Object value = context.getScheduler().getContext().get(CONTEXT_TRIGGER_HANDLER);
            if (!(value instanceof ScheduleTriggerHandler)) {
                throw new IllegalStateException("ScheduleTriggerHandler is not registered in Quartz SchedulerContext");
            }
            ScheduleTriggerContext trigger = new ScheduleTriggerContext();
            trigger.setJobCode(context.getMergedJobDataMap().getString(KEY_JOB_CODE));
            trigger.setParameters(context.getMergedJobDataMap().getString(KEY_PARAMETERS));
            trigger.setProviderId(QuartzSchedulingProvider.PROVIDER_ID);
            trigger.setScheduledTime(context.getScheduledFireTime() == null
                    ? Instant.now() : context.getScheduledFireTime().toInstant());
            ((ScheduleTriggerHandler) value).onTrigger(trigger);
        } catch (Exception ex) {
            throw new JobExecutionException("Failed to process Peach scheduler Quartz trigger", ex, false);
        }
    }
}
