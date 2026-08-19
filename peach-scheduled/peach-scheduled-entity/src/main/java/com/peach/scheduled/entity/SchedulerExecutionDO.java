package com.peach.scheduled.entity;

import com.peach.scheduled.common.ExecutionState;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 调度执行实例数据库对象。
 *
 * <p>一条记录表示任务的一次逻辑触发。分布式执行采用至少一次投递语义，
 * 因此业务 Handler 仍必须具备幂等能力。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Data
@Entity
@Table(name = "PEACH_SCHEDULER_EXECUTION")
@Schema(description = "调度执行实例数据库对象")
public class SchedulerExecutionDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 执行实例唯一标识。 */
    @Id
    @Column(name = "EXECUTION_ID")
    @Schema(description = "执行实例唯一标识")
    private String executionId;

    /** 关联任务主键。 */
    @Column(name = "JOB_ID")
    @Schema(description = "关联任务主键")
    private Long jobId;

    /** 稳定任务编码。 */
    @Column(name = "JOB_CODE")
    @Schema(description = "稳定任务编码")
    private String jobCode;

    /** 逻辑触发唯一键。 */
    @Column(name = "OCCURRENCE_KEY")
    @Schema(description = "逻辑触发唯一键")
    private String occurrenceKey;

    /** 触发来源类型。 */
    @Column(name = "TRIGGER_TYPE")
    @Schema(description = "触发来源类型")
    private String triggerType;

    /** 计划执行时间。 */
    @Column(name = "SCHEDULED_TIME")
    @Schema(description = "计划执行时间")
    private LocalDateTime scheduledTime;

    /** 执行生命周期状态。 */
    @Column(name = "STATE")
    @Schema(description = "执行生命周期状态")
    private ExecutionState state;

    /** 当前尝试次数，从 1 开始。 */
    @Column(name = "ATTEMPT")
    @Schema(description = "当前尝试次数")
    private Integer attempt;

    /** 当前持有执行租约的实例标识。 */
    @Column(name = "EXECUTOR_INSTANCE")
    @Schema(description = "当前持有执行租约的实例标识")
    private String executorInstance;

    /** 当前租约失效时间。 */
    @Column(name = "LEASE_UNTIL")
    @Schema(description = "当前租约失效时间")
    private LocalDateTime leaseUntil;

    /** 下次允许重试时间。 */
    @Column(name = "NEXT_RETRY_TIME")
    @Schema(description = "下次允许重试时间")
    private LocalDateTime nextRetryTime;

    /** 实际开始时间。 */
    @Column(name = "START_TIME")
    @Schema(description = "实际开始时间")
    private LocalDateTime startTime;

    /** 实际结束时间。 */
    @Column(name = "FINISH_TIME")
    @Schema(description = "实际结束时间")
    private LocalDateTime finishTime;

    /** 执行耗时，单位毫秒。 */
    @Column(name = "DURATION_MS")
    @Schema(description = "执行耗时，单位毫秒")
    private Long durationMs;

    /** 链路追踪标识。 */
    @Column(name = "TRACE_ID")
    @Schema(description = "链路追踪标识")
    private String traceId;

    /** 失败类型。 */
    @Column(name = "ERROR_TYPE")
    @Schema(description = "失败类型")
    private String errorType;

    /** 脱敏后的失败摘要。 */
    @Column(name = "ERROR_MESSAGE")
    @Schema(description = "脱敏后的失败摘要")
    private String errorMessage;

    /** 乐观锁版本号。 */
    @Column(name = "VERSION")
    @Schema(description = "乐观锁版本号")
    private Long version;

    /** 创建时间。 */
    @Column(name = "CREATED_TIME")
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    /** 修改时间。 */
    @Column(name = "MODIFY_TIME")
    @Schema(description = "修改时间")
    private LocalDateTime modifyTime;
}
