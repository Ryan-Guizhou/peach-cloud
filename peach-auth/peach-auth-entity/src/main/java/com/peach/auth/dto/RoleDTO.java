package com.peach.auth.dto;

import com.peach.common.PeachGroup;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * 角色维护请求参数。
 * <p>用于角色的新增、修改和授权场景，按租户和机构维度隔离数据。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/9 16:45
 */
@Data
@Schema(description = "角色DTO")
public class RoleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "角色ID")
    @NotBlank(message = "角色ID不能为空", groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    private String roleId;

    @Schema(description = "租户ID")
    @NotBlank(message = "租户ID不能为空", groups = {PeachGroup.InsertGroup.class})
    private String tenantId;

    @Schema(description = "机构ID")
    @NotBlank(message = "机构ID不能为空", groups = {PeachGroup.InsertGroup.class})
    private String orgId;

    @Schema(description = "角色编码")
    @NotBlank(message = "角色编码不能为空", groups = {PeachGroup.InsertGroup.class})
    @Size(max = 50, message = "角色编码长度不能超过50", groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    private String roleCode;

    @Schema(description = "角色名称")
    @NotBlank(message = "角色名称不能为空", groups = {PeachGroup.InsertGroup.class})
    @Size(max = 100, message = "角色名称长度不能超过100", groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    private String roleName;

    @Schema(description = "角色描述")
    @Size(max = 255, message = "角色描述长度不能超过255", groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    private String roleDesc;

    @Schema(description = "角色范围")
    @Size(max = 50, message = "角色范围长度不能超过50", groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    private String roleScope;

    @Schema(description = "角色类型")
    @Size(max = 50, message = "角色类型长度不能超过50", groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    private String roleType;

    @Schema(description = "是否删除")
    private Integer isDelete;

    @Schema(description = "角色登录跳转")
    @Size(max = 255, message = "角色登录跳转长度不能超过255", groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    private String skipUrl;
}
