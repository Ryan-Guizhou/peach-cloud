package com.peach.scheduled.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 调度 Handler 视图对象。
 * <p>该对象只用于接口响应和页面展示，不参与数据库持久化。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Data
@Schema(description = "调度 Handler 视图对象")
public class SchedulerHandlerVO {

    /** 主键。 */
    @Schema(description = "主键")
    private String id;

    /** 业务应用名称。 */
    @Schema(description = "业务应用名称")
    private String applicationName;

    /** Handler 名称。 */
    @Schema(description = "Handler 名称")
    private String handlerName;

    /** Handler 说明。 */
    @Schema(description = "Handler 说明")
    private String description;

    /** 在线状态。 */
    @Schema(description = "在线状态")
    private String status;

    /** 最近一次上报的业务实例标识。 */
    @Schema(description = "最近一次上报的业务实例标识")
    private String instanceId;

    /** 最近一次心跳时间。 */
    @Schema(description = "最近一次心跳时间")
    private LocalDateTime lastHeartbeatTime;

    /** 创建时间。 */
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    /** 修改时间。 */
    @Schema(description = "修改时间")
    private LocalDateTime modifyTime;
}
