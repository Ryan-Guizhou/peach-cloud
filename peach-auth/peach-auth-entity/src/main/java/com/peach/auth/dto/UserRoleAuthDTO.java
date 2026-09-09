package com.peach.auth.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户角色绑定参数。
 */
@Data
@Schema(description = "用户角色绑定参数")
public class UserRoleAuthDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 4829103748291038472L;

    @Schema(description = "租户ID")
    @NotBlank(message = "租户ID不能为空")
    private String tenantId;

    @Schema(description = "机构ID")
    @NotBlank(message = "机构ID不能为空")
    private String orgId;

    @Schema(description = "用户账号")
    @NotBlank(message = "用户账号不能为空")
    private String userCode;

    @Schema(description = "年度")
    @NotNull(message = "年度不能为空")
    private Integer fiscal;

    @Schema(description = "角色编码列表")
    private List<String> roleCodeList;
}
