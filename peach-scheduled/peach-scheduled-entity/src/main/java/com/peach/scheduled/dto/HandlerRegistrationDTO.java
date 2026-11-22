package com.peach.scheduled.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/**
 * 业务服务调度 Handler 注册请求。
 * <p>业务服务启动和心跳阶段通过内部接口上报当前实例支持的 Handler 列表，
 * 调度中心刷新能力心跳，并为首次发现的应用与 Handler 自动登记未启用的默认任务。
 * 重复上报不覆盖任务管理端配置，也不替代服务发现。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Data
@Schema(description = "业务服务调度 Handler 注册请求")
public class HandlerRegistrationDTO {

    /** 业务应用名称。 */
    @NotBlank
    @Size(max = 128)
    @Schema(description = "业务应用名称")
    private String applicationName;

    /** 当前业务服务实例标识。 */
    @NotBlank
    @Size(max = 160)
    @Schema(description = "当前业务服务实例标识")
    private String instanceId;

    /** 当前实例支持的 Handler 列表。 */
    @Valid
    @NotEmpty
    @Schema(description = "当前实例支持的 Handler 列表")
    private List<Item> handlers = new ArrayList<Item>();

    /**
     * 单个 Handler 注册项。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */
    @Data
    @Schema(description = "单个 Handler 注册项")
    public static class Item {
        /** 完整的首次任务定义；旧客户端不传时仅维护 Handler 心跳。 */
        @Valid
        private SchedulerJobSaveDTO task;

        /** Handler 名称。 */
        @NotBlank
        @Size(max = 128)
        @Schema(description = "Handler 名称")
        private String handlerName;

        /** Handler 说明。 */
        @Size(max = 500)
        @Schema(description = "Handler 说明")
        private String description;
    }
}
