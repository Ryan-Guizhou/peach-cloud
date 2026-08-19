package com.peach.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 角色资源授权参数。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 21:00
 */
@Data
@Schema(description = "角色资源授权参数")
public class RoleResourceAuthDTO implements Serializable {
    private static final long serialVersionUID = 1L;

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

    @Schema(description = "资源授权列表")
    @Valid
    private List<RoleResourceItemDTO> resourceList;

    @Data
    @Schema(description = "角色资源授权明细")
    public static class RoleResourceItemDTO implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "功能编码")
        @NotBlank(message = "功能编码不能为空")
        private String funcCode;

        @Schema(description = "操作类型，BUTTON/API")
        @NotBlank(message = "操作类型不能为空")
        private String opType;

        @Schema(description = "资源编码")
        @NotBlank(message = "资源编码不能为空")
        private String resourceCode;

        @Schema(description = "资源名称")
        private String resourceName;
    }
}
