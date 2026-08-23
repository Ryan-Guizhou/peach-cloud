package com.peach.scheduler.model;

import java.time.Instant;

/**
 * 调度引擎无关的数据模型。
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
     * 创建相关对象。
     */
    public JobDefinition() {
    }

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
     * @return 返回结果
     */
    public String getJobCode() {
        return jobCode;
    }
    /**
     * 设置相关数据。
     *
     * @param jobCode 参数说明
     */
    public void setJobCode(String jobCode) {
        this.jobCode = jobCode;
    }
    /**
     * 获取相关数据。
     *
     * @return 返回结果
     */
    public String getApplicationName() {
        return applicationName;
    }
    /**
     * 设置相关数据。
     *
     * @param applicationName 参数说明
     */
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }
    /**
     * 获取相关数据。
     *
     * @return 返回结果
     */
    public String getHandlerName() {
        return handlerName;
    }
    /**
     * 设置相关数据。
     *
     * @param handlerName 参数说明
     */
    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }
    /**
     * 获取相关数据。
     *
     * @return 返回结果
     */
    public ScheduleType getScheduleType() {
        return scheduleType;
    }
    /**
     * 设置相关数据。
     *
     * @param scheduleType 参数说明
     */
    public void setScheduleType(ScheduleType scheduleType) {
        this.scheduleType = scheduleType;
    }
    /**
     * 获取相关数据。
     *
     * @return 返回结果
     */
    public String getCronExpression() {
        return cronExpression;
    }
    /**
     * 设置相关数据。
     *
     * @param cronExpression 参数说明
     */
    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }
    /**
     * 获取相关数据。
     *
     * @return 返回结果
     */
    public long getIntervalSeconds() {
        return intervalSeconds;
    }
    /**
     * 设置相关数据。
     *
     * @param intervalSeconds 参数说明
     */
    public void setIntervalSeconds(long intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }
    /**
     * 获取相关数据。
     *
     * @return 返回结果
     */
    public Instant getStartAt() {
        return startAt;
    }
    /**
     * 设置相关数据。
     *
     * @param startAt 参数说明
     */
    public void setStartAt(Instant startAt) {
        this.startAt = startAt;
    }
    /**
     * 获取相关数据。
     *
     * @return 返回结果
     */
    public String getTimezone() {
        return timezone;
    }
    /**
     * 设置相关数据。
     *
     * @param timezone 参数说明
     */
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
    /**
     * 获取相关数据。
     *
     * @return 返回结果
     */
    public MisfirePolicy getMisfirePolicy() {
        return misfirePolicy;
    }
    /**
     * 设置相关数据。
     *
     * @param misfirePolicy 参数说明
     */
    public void setMisfirePolicy(MisfirePolicy misfirePolicy) {
        this.misfirePolicy = misfirePolicy;
    }
    /**
     * 获取相关数据。
     *
     * @return 返回结果
     */
    public ConcurrencyPolicy getConcurrencyPolicy() {
        return concurrencyPolicy;
    }
    /**
     * 设置相关数据。
     *
     * @param concurrencyPolicy 参数说明
     */
    public void setConcurrencyPolicy(ConcurrencyPolicy concurrencyPolicy) {
        this.concurrencyPolicy = concurrencyPolicy;
    }
    /**
     * 获取相关数据。
     *
     * @return 返回结果
     */
    public String getParameters() {
        return parameters;
    }
    /**
     * 设置相关数据。
     *
     * @param parameters 参数说明
     */
    public void setParameters(String parameters) {
        this.parameters = parameters;
    }
    /**
     * 获取相关数据。
     *
     * @return 返回结果
     */
    public boolean isEnabled() {
        return enabled;
    }
    /**
     * 设置相关数据。
     *
     * @param enabled 参数说明
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
