package com.peach.scheduled.qo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 调度执行实例列表查询条件。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Data
@Schema(description = "调度执行实例列表查询条件")
public class SchedulerExecutionQO {

    /** 执行实例标识。 */
    private String executionId;

    /** 触发来源。 */
    private String triggerType;

    /** 执行器实例。 */
    private String executorInstance;

    /** 计划时间下界，包含。 */
    private String scheduledFrom;

    /** 计划时间上界，包含。 */
    private String scheduledTo;

    /** 解析计划时间下界。 @return 本地时间 */
    public LocalDateTime getScheduledFromValue() {
        return scheduledFrom == null || scheduledFrom.isBlank() ? null : java.time.LocalDateTime.parse(scheduledFrom);
    }

    /** 解析计划时间上界。 @return 本地时间 */
    public LocalDateTime getScheduledToValue() {
        return scheduledTo == null || scheduledTo.isBlank() ? null : java.time.LocalDateTime.parse(scheduledTo);
    }

    /** 页码，从 1 开始。 */
    @Schema(description = "页码，从 1 开始")
    private Integer pageNum = 1;

    /** 每页数量。 */
    @Schema(description = "每页数量")
    private Integer pageSize = 20;

    /** 任务编码。 */
    @Schema(description = "任务编码")
    private String jobCode;

    /** 执行实例状态。 */
    @Schema(description = "执行实例状态")
    private String state;

    /**
     * 获取经过边界修正后的 SQL 分页偏移量。
     *
     * @return 从 0 开始的分页偏移量
     */
    public int getOffset() {
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        return (safePageNum - 1) * getSafePageSize();
    }

    /**
     * 获取经过边界修正后的每页数量。
     *
     * @return 1 到 200 之间的每页数量
     */
    public int getSafePageSize() {
        if (pageSize == null || pageSize < 1) {
            return 20;
        }
        return Math.min(pageSize, 200);
    }
}
