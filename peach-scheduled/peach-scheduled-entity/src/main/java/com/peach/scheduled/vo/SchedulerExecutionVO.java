package com.peach.scheduled.vo;

import com.peach.scheduled.common.ExecutionState;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 调度执行视图对象。
 *
 * <p>该对象只用于接口响应和页面展示，不参与数据库持久化。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Data
@Schema(description = "调度执行视图对象")
public class SchedulerExecutionVO {

    /** 执行实例唯一标识。 */
    @Schema(description = "执行实例唯一标识")
    private String executionId;

    /** 关联任务主键。 */
    @Schema(description = "关联任务主键")
    private Long jobId;

    /** 稳定任务编码。 */
    @Schema(description = "稳定任务编码")
    private String jobCode;

    /** 逻辑触发唯一键。 */
    @Schema(description = "逻辑触发唯一键")
    private String occurrenceKey;

    /** 触发来源类型。 */
    @Schema(description = "触发来源类型")
    private String triggerType;

    /** 计划执行时间。 */
    @Schema(description = "计划执行时间")
    private LocalDateTime scheduledTime;

    /** 执行生命周期状态。 */
    @Schema(description = "执行生命周期状态")
    private ExecutionState state;

    /** 当前尝试次数，从 1 开始。 */
    @Schema(description = "当前尝试次数")
    private Integer attempt;

    /** 当前持有执行租约的实例标识。 */
    @Schema(description = "当前持有执行租约的实例标识")
    private String executorInstance;

    /** 当前租约失效时间。 */
    @Schema(description = "当前租约失效时间")
    private LocalDateTime leaseUntil;

    /** 下次允许重试时间。 */
    @Schema(description = "下次允许重试时间")
    private LocalDateTime nextRetryTime;

    /** 实际开始时间。 */
    @Schema(description = "实际开始时间")
    private LocalDateTime startTime;

    /** 实际结束时间。 */
    @Schema(description = "实际结束时间")
    private LocalDateTime finishTime;

    /** 执行耗时，单位毫秒。 */
    @Schema(description = "执行耗时，单位毫秒")
    private Long durationMs;

    /** 链路追踪标识。 */
    @Schema(description = "链路追踪标识")
    private String traceId;

    /** 失败类型。 */
    @Schema(description = "失败类型")
    private String errorType;

    /** 脱敏后的失败摘要。 */
    @Schema(description = "脱敏后的失败摘要")
    private String errorMessage;

    /** 乐观锁版本号。 */
    @Schema(description = "乐观锁版本号")
    private Long version;

    /** 创建时间。 */
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    /** 修改时间。 */
    @Schema(description = "修改时间")
    private LocalDateTime modifyTime;
}
