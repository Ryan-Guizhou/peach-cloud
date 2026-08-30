package com.peach.scheduled.entity;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 调度任务Version数据对象。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Schema(description = "调度任务版本数据对象")
public class SchedulerJobVersionDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "任务主键")
    private String jobId;

    @Schema(description = "调度定义版本号")
    private Long scheduleVersion;

    @Schema(description = "稳定任务编码")
    private String jobCode;

    @Schema(description = "目标业务应用名称")
    private String applicationName;

    @Schema(description = "业务处理器名称")
    private String handlerName;

    @Schema(description = "调度类型")
    private String scheduleType;

    @Schema(description = "Cron 表达式")
    private String cronExpression;

    @Schema(description = "固定周期秒数")
    private Long intervalSeconds;

    @Schema(description = "调度起始时间")
    private LocalDateTime startAt;

    @Schema(description = "调度时区")
    private String timeZone;

    @Schema(description = "错过触发处理策略")
    private String misfirePolicy;

    @Schema(description = "并发执行策略")
    private String concurrencyPolicy;

    @Schema(description = "单次执行超时时间，单位毫秒")
    private Long timeoutMs;

    @Schema(description = "最大执行次数")
    private Integer maxAttempts;

    @Schema(description = "重试间隔秒数")
    private Integer retryIntervalSeconds;

    @Schema(description = "任务参数 JSON")
    private String parametersJson;

    @Schema(description = "任务生命周期状态")
    private String state;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "创建人ID")
    private String creatorId;

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public Long getScheduleVersion() {
        return scheduleVersion;
    }

    public void setScheduleVersion(Long scheduleVersion) {
        this.scheduleVersion = scheduleVersion;
    }

    public String getJobCode() {
        return jobCode;
    }

    public void setJobCode(String jobCode) {
        this.jobCode = jobCode;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getHandlerName() {
        return handlerName;
    }

    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }

    public String getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(String scheduleType) {
        this.scheduleType = scheduleType;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public Long getIntervalSeconds() {
        return intervalSeconds;
    }

    public void setIntervalSeconds(Long intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getMisfirePolicy() {
        return misfirePolicy;
    }

    public void setMisfirePolicy(String misfirePolicy) {
        this.misfirePolicy = misfirePolicy;
    }

    public String getConcurrencyPolicy() {
        return concurrencyPolicy;
    }

    public void setConcurrencyPolicy(String concurrencyPolicy) {
        this.concurrencyPolicy = concurrencyPolicy;
    }

    public Long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Integer getRetryIntervalSeconds() {
        return retryIntervalSeconds;
    }

    public void setRetryIntervalSeconds(Integer retryIntervalSeconds) {
        this.retryIntervalSeconds = retryIntervalSeconds;
    }

    public String getParametersJson() {
        return parametersJson;
    }

    public void setParametersJson(String parametersJson) {
        this.parametersJson = parametersJson;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }
}
