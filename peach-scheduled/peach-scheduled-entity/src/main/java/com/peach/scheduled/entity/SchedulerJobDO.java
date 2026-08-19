package com.peach.scheduled.entity;

import com.peach.scheduled.common.JobState;
import com.peach.scheduled.common.SyncStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 调度任务定义数据库对象。
 *
 * <p>该对象与 PEACH_SCHEDULER_JOB 表一一对应，仅承载持久化字段，
 * 不在 DO 中放置调度、重试、状态迁移等业务逻辑。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Data
@Entity
@Table(name = "PEACH_SCHEDULER_JOB")
@Schema(description = "调度任务定义数据库对象")
public class SchedulerJobDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 任务主键。 */
    @Id
    @Column(name = "ID")
    @Schema(description = "任务主键")
    private Long id;

    /** 稳定任务编码。 */
    @Column(name = "JOB_CODE")
    @Schema(description = "稳定任务编码")
    private String jobCode;

    /** 任务名称。 */
    @Column(name = "JOB_NAME")
    @Schema(description = "任务名称")
    private String jobName;

    /** 目标业务应用名称。 */
    @Column(name = "APPLICATION_NAME")
    @Schema(description = "目标业务应用名称")
    private String applicationName;

    /** 业务处理器名称。 */
    @Column(name = "HANDLER_NAME")
    @Schema(description = "业务处理器名称")
    private String handlerName;

    /** 任务说明。 */
    @Column(name = "DESCRIPTION")
    @Schema(description = "任务说明")
    private String description;

    /** 调度类型。 */
    @Column(name = "SCHEDULE_TYPE")
    @Schema(description = "调度类型")
    private String scheduleType;

    /** Cron 表达式。 */
    @Column(name = "CRON_EXPRESSION")
    @Schema(description = "Cron 表达式")
    private String cronExpression;

    /** 固定周期秒数。 */
    @Column(name = "INTERVAL_SECONDS")
    @Schema(description = "固定周期秒数")
    private Long intervalSeconds;

    /** 一次性任务或固定调度的起始时间。 */
    @Column(name = "START_AT")
    @Schema(description = "调度起始时间")
    private LocalDateTime startAt;

    /** 调度时区。 */
    @Column(name = "TIME_ZONE")
    @Schema(description = "调度时区")
    private String timeZone;

    /** 错过触发处理策略。 */
    @Column(name = "MISFIRE_POLICY")
    @Schema(description = "错过触发处理策略")
    private String misfirePolicy;

    /** 并发执行策略。 */
    @Column(name = "CONCURRENCY_POLICY")
    @Schema(description = "并发执行策略")
    private String concurrencyPolicy;

    /** 单次执行超时时间，单位毫秒。 */
    @Column(name = "TIMEOUT_MS")
    @Schema(description = "单次执行超时时间，单位毫秒")
    private Long timeoutMs;

    /** 最大执行次数，包含首次执行。 */
    @Column(name = "MAX_ATTEMPTS")
    @Schema(description = "最大执行次数")
    private Integer maxAttempts;

    /** 重试间隔秒数。 */
    @Column(name = "RETRY_INTERVAL_SECONDS")
    @Schema(description = "重试间隔秒数")
    private Integer retryIntervalSeconds;

    /** 任务参数 JSON。 */
    @Column(name = "PARAMETERS_JSON")
    @Schema(description = "任务参数 JSON")
    private String parametersJson;

    /** 任务生命周期状态。 */
    @Column(name = "STATE")
    @Schema(description = "任务生命周期状态")
    private JobState state;

    /** 调度定义版本号。 */
    @Column(name = "SCHEDULE_VERSION")
    @Schema(description = "调度定义版本号")
    private Long scheduleVersion;

    /** 调度引擎同步状态。 */
    @Column(name = "SYNC_STATUS")
    @Schema(description = "调度引擎同步状态")
    private SyncStatus syncStatus;

    /** 最近一次同步失败摘要。 */
    @Column(name = "LAST_SYNC_ERROR")
    @Schema(description = "最近一次同步失败摘要")
    private String lastSyncError;

    /** 下次计划触发时间。 */
    @Column(name = "NEXT_FIRE_TIME")
    @Schema(description = "下次计划触发时间")
    private LocalDateTime nextFireTime;

    /** 最近一次计划触发时间。 */
    @Column(name = "LAST_FIRE_TIME")
    @Schema(description = "最近一次计划触发时间")
    private LocalDateTime lastFireTime;

    /** 乐观锁版本号。 */
    @Column(name = "VERSION")
    @Schema(description = "乐观锁版本号")
    private Long version;

    /** 创建人 ID。 */
    @Column(name = "CREATOR_ID")
    @Schema(description = "创建人 ID")
    private String creatorId;

    /** 创建时间。 */
    @Column(name = "CREATED_TIME")
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    /** 修改人 ID。 */
    @Column(name = "MODIFIER_ID")
    @Schema(description = "修改人 ID")
    private String modifierId;

    /** 修改时间。 */
    @Column(name = "MODIFY_TIME")
    @Schema(description = "修改时间")
    private LocalDateTime modifyTime;
}
