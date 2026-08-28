package com.peach.auth.dto;

import java.io.Serial;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 角色功能授权参数。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 21:00
 */
@Data
@Schema(description = "角色功能授权参数")
public class RoleFunctionAuthDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 6984993053452352110L;

    @Schema(description = "租户ID")
    @NotBlank(message = "租户ID不能为空")
    private String tenantId;

    @Schema(description = "机构ID")
    @NotBlank(message = "机构ID不能为空")
    private String orgId;

    @Schema(description = "角色编码")
    @NotBlank(message = "角色编码不能为空")
    private String partyCode;

    @Schema(description = "年度")
    @NotNull(message = "年度不能为空")
    private Integer fiscal;

    @Schema(description = "应用ID")
    private String appId;

    @Schema(description = "功能编码列表")
    private List<String> funcCodeList;
}
