package com.peach.scheduled.entity;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 调度执行 Attempt 数据对象。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Schema(description = "调度执行尝试数据对象")
public class SchedulerExecutionAttemptDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "执行实例唯一标识")
    private String executionId;

    @Schema(description = "尝试次数")
    private Integer attemptNo;

    @Schema(description = "执行器实例标识")
    private String executorInstance;

    @Schema(description = "尝试状态")
    private String state;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime finishTime;

    @Schema(description = "执行耗时，单位毫秒")
    private Long durationMs;

    @Schema(description = "失败类型")
    private String errorType;

    @Schema(description = "脱敏后的失败摘要")
    private String errorMessage;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public Integer getAttemptNo() {
        return attemptNo;
    }

    public void setAttemptNo(Integer attemptNo) {
        this.attemptNo = attemptNo;
    }

    public String getExecutorInstance() {
        return executorInstance;
    }

    public void setExecutorInstance(String executorInstance) {
        this.executorInstance = executorInstance;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(LocalDateTime finishTime) {
        this.finishTime = finishTime;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }
}
