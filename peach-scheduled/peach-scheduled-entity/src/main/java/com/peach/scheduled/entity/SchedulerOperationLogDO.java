package com.peach.scheduled.entity;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 调度Operation日志数据对象。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Schema(description = "调度操作日志数据对象")
public class SchedulerOperationLogDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "操作类型")
    private String operation;

    @Schema(description = "操作目标类型")
    private String targetType;

    @Schema(description = "操作目标ID")
    private String targetId;

    @Schema(description = "操作人ID")
    private String operatorId;

    @Schema(description = "操作原因")
    private String reason;

    @Schema(description = "操作结果")
    private String result;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }
}
