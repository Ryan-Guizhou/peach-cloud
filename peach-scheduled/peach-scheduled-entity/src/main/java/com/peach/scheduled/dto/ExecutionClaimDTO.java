package com.peach.scheduled.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 调度执行实例租约抢占请求对象。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Data
@Schema(description = "调度执行实例租约抢占请求")
public class ExecutionClaimDTO {

    /** 执行器实例唯一标识。 */
    @NotBlank
    @Size(max = 160)
    @Schema(description = "执行器实例唯一标识")
    private String executorInstance;
}
