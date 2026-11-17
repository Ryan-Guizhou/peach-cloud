package com.peach.auth.dto;

import java.io.Serial;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 切换当前租户机构上下文参数。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 20:30
 */
@Data
@Schema(description = "切换当前租户机构上下文参数")
public class SwitchContextDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -4932451809499849490L;

    @Schema(description = "目标租户物理 ID")
    @NotBlank(message = "租户物理 ID 不能为空")
    private String tenantRefId;

    @Schema(description = "目标应用物理 ID")
    @NotBlank(message = "应用物理 ID 不能为空")
    private String appRefId;
}
