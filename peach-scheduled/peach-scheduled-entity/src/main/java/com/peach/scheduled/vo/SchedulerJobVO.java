package com.peach.scheduled.vo;

import com.peach.scheduled.common.JobState;
import com.peach.scheduled.common.SyncStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 调度任务视图对象。
 *
 * <p>该对象只用于接口响应和页面展示，不参与数据库持久化。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Data
@Schema(description = "调度任务视图对象")
public class SchedulerJobVO {

    /** 任务主键。 */
    @Schema(description = "任务主键")
    private Long id;

    /** 稳定任务编码。 */
    @Schema(description = "稳定任务编码")
    private String jobCode;

    /** 任务名称。 */
    @Schema(description = "任务名称")
    private String jobName;

    /** 目标业务应用名称。 */
    @Schema(description = "目标业务应用名称")
    private String applicationName;

    /** 业务处理器名称。 */
    @Schema(description = "业务处理器名称")
    private String handlerName;

    /** 任务说明。 */
    @Schema(description = "任务说明")
    private String description;

    /** 调度类型。 */
    @Schema(description = "调度类型")
    private String scheduleType;

    /** Cron 表达式。 */
    @Schema(description = "Cron 表达式")
    private String cronExpression;

    /** 固定周期秒数。 */
    @Schema(description = "固定周期秒数")
    private Long intervalSeconds;

    /** 一次性任务或固定调度的起始时间。 */
    @Schema(description = "调度起始时间")
    private LocalDateTime startAt;

    /** 调度时区。 */
    @Schema(description = "调度时区")
    private String timeZone;

    /** 错过触发处理策略。 */
    @Schema(description = "错过触发处理策略")
    private String misfirePolicy;

    /** 并发执行策略。 */
    @Schema(description = "并发执行策略")
    private String concurrencyPolicy;

    /** 单次执行超时时间，单位毫秒。 */
    @Schema(description = "单次执行超时时间，单位毫秒")
    private Long timeoutMs;

    /** 最大执行次数，包含首次执行。 */
    @Schema(description = "最大执行次数")
    private Integer maxAttempts;

    /** 重试间隔秒数。 */
    @Schema(description = "重试间隔秒数")
    private Integer retryIntervalSeconds;

    /** 任务参数 JSON。 */
    @Schema(description = "任务参数 JSON")
    private String parametersJson;

    /** 任务生命周期状态。 */
    @Schema(description = "任务生命周期状态")
    private JobState state;

    /** 调度定义版本号。 */
    @Schema(description = "调度定义版本号")
    private Long scheduleVersion;

    /** 调度引擎同步状态。 */
    @Schema(description = "调度引擎同步状态")
    private SyncStatus syncStatus;

    /** 最近一次同步失败摘要。 */
    @Schema(description = "最近一次同步失败摘要")
    private String lastSyncError;

    /** 下次计划触发时间。 */
    @Schema(description = "下次计划触发时间")
    private LocalDateTime nextFireTime;

    /** 最近一次计划触发时间。 */
    @Schema(description = "最近一次计划触发时间")
    private LocalDateTime lastFireTime;

    /** 乐观锁版本号。 */
    @Schema(description = "乐观锁版本号")
    private Long version;

    /** 创建人 ID。 */
    @Schema(description = "创建人 ID")
    private String creatorId;

    /** 创建时间。 */
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    /** 修改人 ID。 */
    @Schema(description = "修改人 ID")
    private String modifierId;

    /** 修改时间。 */
    @Schema(description = "修改时间")
    private LocalDateTime modifyTime;
}
