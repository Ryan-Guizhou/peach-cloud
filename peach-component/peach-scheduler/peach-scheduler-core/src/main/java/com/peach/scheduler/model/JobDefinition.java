package com.peach.scheduler.model;

import java.time.Instant;

/**
 * 调度任务定义。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public class JobDefinition {
    private String jobCode;
    private String applicationName;
    private String handlerName;
    private ScheduleType scheduleType;
    private String cronExpression;
    private long intervalSeconds;
    private Instant startAt;
    private String timezone = "Asia/Shanghai";
    private MisfirePolicy misfirePolicy = MisfirePolicy.FIRE_ONCE_NOW;
    private ConcurrencyPolicy concurrencyPolicy = ConcurrencyPolicy.DISALLOW;
    private String parameters;
    private boolean enabled;

    /**
     * 校验调度定义必填字段和不同调度类型的参数约束。
     *
     * @throws IllegalArgumentException 调度定义不完整或参数非法时抛出。
     */
    public void validate() {
        requireText(jobCode, "jobCode");
        requireText(applicationName, "applicationName");
        requireText(handlerName, "handlerName");
        if (scheduleType == null) throw new IllegalArgumentException("scheduleType is required");
        if (scheduleType == ScheduleType.CRON) requireText(cronExpression, "cronExpression");
        if (scheduleType == ScheduleType.FIXED_INTERVAL && intervalSeconds <= 0) {
            throw new IllegalArgumentException("intervalSeconds must be positive");
        }
        if (scheduleType == ScheduleType.ONE_TIME && startAt == null) {
            throw new IllegalArgumentException("startAt is required for ONE_TIME schedule");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
    }

    /**
     * 获取任务编码。
     *
     * @return 任务编码。
     */
    public String getJobCode() {
        return jobCode;
    }
    /**
     * 设置任务编码。
     *
     * @param jobCode 任务编码。
     */
    public void setJobCode(String jobCode) {
        this.jobCode = jobCode;
    }
    /**
     * 获取目标应用名称。
     *
     * @return 目标应用名称。
     */
    public String getApplicationName() {
        return applicationName;
    }
    /**
     * 设置目标应用名称。
     *
     * @param applicationName 目标应用名称。
     */
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }
    /**
     * 获取业务处理器名称。
     *
     * @return 业务处理器名称。
     */
    public String getHandlerName() {
        return handlerName;
    }
    /**
     * 设置业务处理器名称。
     *
     * @param handlerName 业务处理器名称。
     */
    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }
    /**
     * 获取调度类型。
     *
     * @return 调度类型。
     */
    public ScheduleType getScheduleType() {
        return scheduleType;
    }
    /**
     * 设置调度类型。
     *
     * @param scheduleType 调度类型。
     */
    public void setScheduleType(ScheduleType scheduleType) {
        this.scheduleType = scheduleType;
    }
    /**
     * 获取 cron 表达式。
     *
     * @return cron 表达式。
     */
    public String getCronExpression() {
        return cronExpression;
    }
    /**
     * 设置 cron 表达式。
     *
     * @param cronExpression cron 表达式。
     */
    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }
    /**
     * 获取固定间隔秒数。
     *
     * @return 固定间隔秒数。
     */
    public long getIntervalSeconds() {
        return intervalSeconds;
    }
    /**
     * 设置固定间隔秒数。
     *
     * @param intervalSeconds 固定间隔秒数。
     */
    public void setIntervalSeconds(long intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }
    /**
     * 获取一次性调度开始时间。
     *
     * @return 一次性调度开始时间。
     */
    public Instant getStartAt() {
        return startAt;
    }
    /**
     * 设置一次性调度开始时间。
     *
     * @param startAt 一次性调度开始时间。
     */
    public void setStartAt(Instant startAt) {
        this.startAt = startAt;
    }
    /**
     * 获取调度时区。
     *
     * @return 调度时区。
     */
    public String getTimezone() {
        return timezone;
    }
    /**
     * 设置调度时区。
     *
     * @param timezone 调度时区。
     */
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
    /**
     * 获取错过触发时的处理策略。
     *
     * @return 错过触发处理策略。
     */
    public MisfirePolicy getMisfirePolicy() {
        return misfirePolicy;
    }
    /**
     * 设置错过触发时的处理策略。
     *
     * @param misfirePolicy 错过触发处理策略。
     */
    public void setMisfirePolicy(MisfirePolicy misfirePolicy) {
        this.misfirePolicy = misfirePolicy;
    }
    /**
     * 获取并发触发策略。
     *
     * @return 并发触发策略。
     */
    public ConcurrencyPolicy getConcurrencyPolicy() {
        return concurrencyPolicy;
    }
    /**
     * 设置并发触发策略。
     *
     * @param concurrencyPolicy 并发触发策略。
     */
    public void setConcurrencyPolicy(ConcurrencyPolicy concurrencyPolicy) {
        this.concurrencyPolicy = concurrencyPolicy;
    }
    /**
     * 获取任务参数。
     *
     * @return 任务参数。
     */
    public String getParameters() {
        return parameters;
    }
    /**
     * 设置任务参数。
     *
     * @param parameters 任务参数。
     */
    public void setParameters(String parameters) {
        this.parameters = parameters;
    }
    /**
     * 判断任务是否启用。
     *
     * @return 启用返回 {@code true}。
     */
    public boolean isEnabled() {
        return enabled;
    }
    /**
     * 设置任务启用状态。
     *
     * @param enabled 任务启用状态。
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
