package com.peach.scheduled.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 调度业务处理器注册数据库对象。
 *
 * <p>该表记录业务服务主动上报的 Handler 能力和实例心跳，
 * 仅用于调度治理和页面展示，不替代服务注册中心。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Data
@Entity
@Table(name = "PEACH_SCHEDULER_HANDLER")
@Schema(description = "调度业务处理器注册数据库对象")
public class SchedulerHandlerDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键。 */
    @Id
    @Column(name = "ID")
    @Schema(description = "主键")
    private Long id;

    /** 业务应用名称。 */
    @Column(name = "APPLICATION_NAME")
    @Schema(description = "业务应用名称")
    private String applicationName;

    /** Handler 名称。 */
    @Column(name = "HANDLER_NAME")
    @Schema(description = "Handler 名称")
    private String handlerName;

    /** Handler 说明。 */
    @Column(name = "DESCRIPTION")
    @Schema(description = "Handler 说明")
    private String description;

    /** 在线状态。 */
    @Column(name = "STATUS")
    @Schema(description = "在线状态")
    private String status;

    /** 最近一次上报的业务实例标识。 */
    @Column(name = "INSTANCE_ID")
    @Schema(description = "最近一次上报的业务实例标识")
    private String instanceId;

    /** 最近一次心跳时间。 */
    @Column(name = "LAST_HEARTBEAT_TIME")
    @Schema(description = "最近一次心跳时间")
    private LocalDateTime lastHeartbeatTime;

    /** 创建时间。 */
    @Column(name = "CREATED_TIME")
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    /** 修改时间。 */
    @Column(name = "MODIFY_TIME")
    @Schema(description = "修改时间")
    private LocalDateTime modifyTime;
}
