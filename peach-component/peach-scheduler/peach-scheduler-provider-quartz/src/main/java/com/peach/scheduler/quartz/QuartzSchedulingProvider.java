package com.peach.scheduler.quartz;

import org.springframework.stereotype.Indexed;

import com.peach.scheduler.exception.SchedulerConfigurationException;
import com.peach.scheduler.exception.SchedulerException;
import com.peach.scheduler.model.ConcurrencyPolicy;
import com.peach.scheduler.model.JobDefinition;
import com.peach.scheduler.model.MisfirePolicy;
import com.peach.scheduler.model.ScheduleType;
import com.peach.scheduler.model.SchedulerCapability;
import com.peach.scheduler.provider.SchedulingProvider;
import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.peach.scheduler.quartz.internal.QuartzDateBridge;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.TimeZone;

/**
 * QuartzScheduling提供者。
 * <p>该实现负责将统一 {@link JobDefinition} 映射为 Quartz JobDetail/Trigger，并提供动态创建、
 * 更新、暂停、恢复、删除和手动触发能力。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
public class QuartzSchedulingProvider implements SchedulingProvider {

    private static final Logger log = LoggerFactory.getLogger(QuartzSchedulingProvider.class);

    /**
     * Quartz Provider 稳定标识。
     */
    public static final String PROVIDER_ID = "quartz";

    private static final Set<SchedulerCapability> CAPABILITIES = Collections.unmodifiableSet(
            EnumSet.of(SchedulerCapability.DYNAMIC_SCHEDULE,
                    SchedulerCapability.PAUSE_RESUME,
                    SchedulerCapability.MANUAL_TRIGGER));

    private final Scheduler scheduler;
    private final PeachQuartzProperties properties;

    /**
     * 创建 Quartz Provider。
     *
     * @param scheduler scheduler。
     * @param properties Peach Quartz 配置
     */
    public QuartzSchedulingProvider(Scheduler scheduler, PeachQuartzProperties properties) {
        this.scheduler = scheduler;
        this.properties = properties;
    }

    /**
     * 接口实现。
     */
    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }

    /**
     * 接口实现。
     */
    @Override
    public Set<SchedulerCapability> getCapabilities() {
        return CAPABILITIES;
    }

    /**
     * 接口实现。
     */
    @Override
    public void schedule(JobDefinition definition) {
        definition.validate();
        try {
            JobKey jobKey = jobKey(definition.getJobCode());
            if (scheduler.checkExists(jobKey)) {
                throw new SchedulerException("Quartz job already exists: " + definition.getJobCode());
            }
            scheduler.scheduleJob(buildJobDetail(definition), buildTrigger(definition));
            if (!definition.isEnabled()) {
                scheduler.pauseJob(jobKey);
            }
            log.info("Quartz job scheduled, jobCode={}, scheduleType={}, enabled={}",
                    definition.getJobCode(), definition.getScheduleType(), definition.isEnabled());
        } catch (org.quartz.SchedulerException ex) {
            throw new SchedulerException("Failed to schedule Quartz job: "
                    + definition.getJobCode(), ex);
        }
    }

    /**
     * 接口实现。
     */
    @Override
    public void reschedule(JobDefinition definition) {
        definition.validate();
        try {
            JobKey key = jobKey(definition.getJobCode());
            if (!scheduler.checkExists(key)) {
                schedule(definition);
                return;
            }

            JobDetail jobDetail = buildJobDetail(definition);
            Trigger trigger = buildTrigger(definition);
            scheduler.addJob(jobDetail, true, true);
            if (scheduler.checkExists(trigger.getKey())) {
                scheduler.rescheduleJob(trigger.getKey(), trigger);
            } else {
                scheduler.scheduleJob(trigger);
            }

            if (definition.isEnabled()) {
                scheduler.resumeJob(key);
            } else {
                scheduler.pauseJob(key);
            }
            log.info("Quartz job rescheduled, jobCode={}, scheduleType={}, enabled={}",
                    definition.getJobCode(), definition.getScheduleType(), definition.isEnabled());
        } catch (org.quartz.SchedulerException ex) {
            throw new SchedulerException("Failed to reschedule Quartz job: "
                    + definition.getJobCode(), ex);
        }
    }

    /**
     * 接口实现。
     */
    @Override
    public void pause(String jobCode) {
        call(jobCode, () -> scheduler.pauseJob(jobKey(jobCode)), "pause");
    }

    /**
     * 接口实现。
     */
    @Override
    public void resume(String jobCode) {
        call(jobCode, () -> scheduler.resumeJob(jobKey(jobCode)), "resume");
    }

    /**
     * 接口实现。
     */
    @Override
    public void delete(String jobCode) {
        call(jobCode, () -> scheduler.deleteJob(jobKey(jobCode)), "delete");
    }

    /**
     * 接口实现。
     */
    @Override
    public void trigger(String jobCode, String parameters) {
        try {
            JobDataMap data = new JobDataMap();
            data.put(AbstractPeachQuartzJob.KEY_PARAMETERS, parameters);
            scheduler.triggerJob(jobKey(jobCode), data);
            log.info("Quartz job manual trigger requested, jobCode={}", jobCode);
        } catch (org.quartz.SchedulerException ex) {
            throw new SchedulerException("Failed to manually trigger Quartz job: " + jobCode, ex);
        }
    }

    /**
     * 接口实现。
     */
    @Override
    public boolean exists(String jobCode) {
        try {
            return scheduler.checkExists(jobKey(jobCode));
        } catch (org.quartz.SchedulerException ex) {
            throw new SchedulerException("Failed to query Quartz job: " + jobCode, ex);
        }
    }

    private JobDetail buildJobDetail(JobDefinition definition) {
        Class<? extends org.quartz.Job> jobClass =
                definition.getConcurrencyPolicy() == ConcurrencyPolicy.DISALLOW
                        ? PeachQuartzDisallowConcurrentJob.class
                        : PeachQuartzConcurrentJob.class;
        JobDataMap data = new JobDataMap();
        data.put(AbstractPeachQuartzJob.KEY_JOB_CODE, definition.getJobCode());
        data.put(AbstractPeachQuartzJob.KEY_PARAMETERS, definition.getParameters());
        return JobBuilder.newJob(jobClass)
                .withIdentity(jobKey(definition.getJobCode()))
                .usingJobData(data)
                .build();
    }

    private Trigger buildTrigger(JobDefinition definition) {
        TriggerBuilder<Trigger> builder = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey(definition.getJobCode()))
                .forJob(jobKey(definition.getJobCode()));
        if (definition.getScheduleType() == ScheduleType.CRON) {
            if (!CronExpression.isValidExpression(definition.getCronExpression())) {
                throw new SchedulerConfigurationException(
                        "Invalid Quartz cron expression for " + definition.getJobCode());
            }
            CronScheduleBuilder schedule = CronScheduleBuilder
                    .cronSchedule(definition.getCronExpression())
                    .inTimeZone(TimeZone.getTimeZone(definition.getTimezone()));
            schedule = definition.getMisfirePolicy() == MisfirePolicy.SKIP
                    ? schedule.withMisfireHandlingInstructionDoNothing()
                    : schedule.withMisfireHandlingInstructionFireAndProceed();
            return builder.withSchedule(schedule).build();
        }
        if (definition.getScheduleType() == ScheduleType.FIXED_INTERVAL) {
            long intervalSeconds = definition.getIntervalSeconds();
            if (intervalSeconds > Integer.MAX_VALUE) {
                throw new SchedulerConfigurationException(
                        "Fixed interval exceeds Quartz supported seconds range for " + definition.getJobCode());
            }
            SimpleScheduleBuilder schedule = SimpleScheduleBuilder.simpleSchedule()
                    .withIntervalInSeconds((int) intervalSeconds)
                    .repeatForever();
            schedule = definition.getMisfirePolicy() == MisfirePolicy.SKIP
                    ? schedule.withMisfireHandlingInstructionNextWithRemainingCount()
                    : schedule.withMisfireHandlingInstructionFireNow();
            return builder.withSchedule(schedule).startNow().build();
        }
        return builder.startAt(QuartzDateBridge.toQuartzDate(definition.getStartAt())).build();
    }

    private JobKey jobKey(String jobCode) {
        return new JobKey(jobCode, properties.getGroup());
    }

    private TriggerKey triggerKey(String jobCode) {
        return new TriggerKey(jobCode, properties.getGroup());
    }

    private void call(String jobCode, QuartzCall call, String action) {
        try {
            call.run();
            log.info("Quartz job action completed, action={}, jobCode={}", action, jobCode);
        } catch (org.quartz.SchedulerException ex) {
            throw new SchedulerException("Failed to " + action + " Quartz job: " + jobCode, ex);
        }
    }

    /**
     * QuartzCall接口。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */
    private interface QuartzCall {

        /**
         * 执行 Quartz 调用。
         *
         * @throws org.quartz.SchedulerException Quartz 调度异常
         */
        void run() throws org.quartz.SchedulerException;
    }
}
