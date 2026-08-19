package com.peach.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 登录后切换当前租户机构上下文参数。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 20:30
 */
@Data
@Schema(description = "切换当前租户机构上下文参数")
public class SwitchContextDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "年度")
    @NotNull(message = "年度不能为空")
    private Integer fiscal;

    @Schema(description = "租户ID")
    @NotBlank(message = "租户ID不能为空")
    private String tenantId;

    @Schema(description = "机构ID")
    @NotBlank(message = "机构ID不能为空")
    private String orgId;
}
