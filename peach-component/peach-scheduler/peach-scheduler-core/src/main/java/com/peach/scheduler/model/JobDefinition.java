package com.peach.scheduler.model;

import java.time.Instant;

/**
 * JobDefinition相关类。
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
     * 校验相关数据。
     * @throws IllegalArgumentException 异常说明
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
     * 获取相关数据。
     *
     * @return 执行结果。
     */
    public String getJobCode() {
        return jobCode;
    }
    /**
     * 设置相关数据。
     *
     * @param jobCode job Code。
     */
    public void setJobCode(String jobCode) {
        this.jobCode = jobCode;
    }
    /**
     * 获取相关数据。
     *
     * @return 执行结果。
     */
    public String getApplicationName() {
        return applicationName;
    }
    /**
     * 设置相关数据。
     *
     * @param applicationName application Name。
     */
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }
    /**
     * 获取相关数据。
     *
     * @return 执行结果。
     */
    public String getHandlerName() {
        return handlerName;
    }
    /**
     * 设置相关数据。
     *
     * @param handlerName handler Name。
     */
    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }
    /**
     * 获取相关数据。
     *
     * @return 执行结果。
     */
    public ScheduleType getScheduleType() {
        return scheduleType;
    }
    /**
     * 设置相关数据。
     *
     * @param scheduleType schedule Type。
     */
    public void setScheduleType(ScheduleType scheduleType) {
        this.scheduleType = scheduleType;
    }
    /**
     * 获取相关数据。
     *
     * @return 执行结果。
     */
    public String getCronExpression() {
        return cronExpression;
    }
    /**
     * 设置相关数据。
     *
     * @param cronExpression cron Expression。
     */
    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }
    /**
     * 获取相关数据。
     *
     * @return 执行结果。
     */
    public long getIntervalSeconds() {
        return intervalSeconds;
    }
    /**
     * 设置相关数据。
     *
     * @param intervalSeconds interval Seconds。
     */
    public void setIntervalSeconds(long intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }
    /**
     * 获取相关数据。
     *
     * @return 执行结果。
     */
    public Instant getStartAt() {
        return startAt;
    }
    /**
     * 设置相关数据。
     *
     * @param startAt start At。
     */
    public void setStartAt(Instant startAt) {
        this.startAt = startAt;
    }
    /**
     * 获取相关数据。
     *
     * @return 执行结果。
     */
    public String getTimezone() {
        return timezone;
    }
    /**
     * 设置相关数据。
     *
     * @param timezone timezone。
     */
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
    /**
     * 获取相关数据。
     *
     * @return 执行结果。
     */
    public MisfirePolicy getMisfirePolicy() {
        return misfirePolicy;
    }
    /**
     * 设置相关数据。
     *
     * @param misfirePolicy misfire Policy。
     */
    public void setMisfirePolicy(MisfirePolicy misfirePolicy) {
        this.misfirePolicy = misfirePolicy;
    }
    /**
     * 获取相关数据。
     *
     * @return 执行结果。
     */
    public ConcurrencyPolicy getConcurrencyPolicy() {
        return concurrencyPolicy;
    }
    /**
     * 设置相关数据。
     *
     * @param concurrencyPolicy concurrency Policy。
     */
    public void setConcurrencyPolicy(ConcurrencyPolicy concurrencyPolicy) {
        this.concurrencyPolicy = concurrencyPolicy;
    }
    /**
     * 获取相关数据。
     *
     * @return 执行结果。
     */
    public String getParameters() {
        return parameters;
    }
    /**
     * 设置相关数据。
     *
     * @param parameters parameters。
     */
    public void setParameters(String parameters) {
        this.parameters = parameters;
    }
    /**
     * 获取相关数据。
     *
     * @return 执行结果。
     */
    public boolean isEnabled() {
        return enabled;
    }
    /**
     * 设置相关数据。
     *
     * @param enabled enabled。
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
