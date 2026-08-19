package com.peach.scheduled.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 调度任务新增或修改请求对象。
 *
 * <p>只承载控制面允许用户配置的任务定义字段，不允许客户端提交内部状态、
 * 乐观锁版本、执行租约等运行时字段。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Data
@Schema(description = "调度任务新增或修改请求")
public class SchedulerJobSaveDTO {

    /** 任务编码。 */
    @NotBlank
    @Size(max = 64)
    @Schema(description = "任务编码")
    private String jobCode;

    /** 任务名称。 */
    @NotBlank
    @Size(max = 128)
    @Schema(description = "任务名称")
    private String jobName;

    /** 目标业务应用名称。 */
    @NotBlank
    @Size(max = 128)
    @Schema(description = "目标业务应用名称")
    private String applicationName;

    /** 业务 Handler 名称。 */
    @NotBlank
    @Size(max = 128)
    @Schema(description = "业务 Handler 名称")
    private String handlerName;

    /** 任务说明。 */
    @Size(max = 500)
    @Schema(description = "任务说明")
    private String description;

    /** 调度类型。 */
    @NotBlank
    @Size(max = 32)
    @Schema(description = "调度类型")
    private String scheduleType;

    /** Quartz Cron 表达式。 */
    @Size(max = 128)
    @Schema(description = "Quartz Cron 表达式")
    private String cronExpression;

    /** 固定周期秒数。 */
    @Schema(description = "固定周期秒数")
    private Long intervalSeconds;

    /** 调度起始时间，使用 ISO 日期时间字符串。 */
    @Size(max = 64)
    @Schema(description = "调度起始时间")
    private String startAt;

    /** 调度时区。 */
    @NotBlank
    @Size(max = 64)
    @Schema(description = "调度时区")
    private String timeZone = "Asia/Shanghai";

    /** 错过触发处理策略。 */
    @NotBlank
    @Size(max = 32)
    @Schema(description = "错过触发处理策略")
    private String misfirePolicy = "FIRE_ONCE_NOW";

    /** 并发执行策略。 */
    @NotBlank
    @Size(max = 32)
    @Schema(description = "并发执行策略")
    private String concurrencyPolicy = "DISALLOW";

    /** 单次执行超时时间，单位毫秒。 */
    @NotNull
    @Min(1000)
    @Max(86400000)
    @Schema(description = "单次执行超时时间，单位毫秒")
    private Long timeoutMs = 1800000L;

    /** 最大执行次数，包含首次执行。 */
    @NotNull
    @Min(1)
    @Max(20)
    @Schema(description = "最大执行次数，包含首次执行")
    private Integer maxAttempts = 1;

    /** 重试间隔秒数。 */
    @NotNull
    @Min(1)
    @Max(86400)
    @Schema(description = "重试间隔秒数")
    private Integer retryIntervalSeconds = 60;

    /** 任务参数 JSON。 */
    @Size(max = 16384)
    @Schema(description = "任务参数 JSON")
    private String parametersJson = "{}";
}
