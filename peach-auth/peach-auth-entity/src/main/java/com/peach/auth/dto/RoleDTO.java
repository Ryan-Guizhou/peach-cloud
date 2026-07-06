package com.peach.auth.dto;

import com.peach.auth.group.RoleGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/9 16:45
 * @Description 角色DTO
 */
@Data
@Schema(description = "角色DTO")
public class RoleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "角色ID")
    @NotBlank(message = "角色ID不能为空", groups = {RoleGroup.insertGroup.class, RoleGroup.updateGroup.class})
    private String roleId;

    @Schema(description = "角色编码")
    @NotBlank(message = "角色编码不能为空", groups = {RoleGroup.insertGroup.class})
    @Size(max = 50, message = "角色编码长度不能超过50", groups = {RoleGroup.insertGroup.class, RoleGroup.updateGroup.class})
    private String roleCode;

    @Schema(description = "角色名称")
    @NotBlank(message = "角色名称不能为空", groups = {RoleGroup.insertGroup.class})
    @Size(max = 100, message = "角色名称长度不能超过100", groups = {RoleGroup.insertGroup.class, RoleGroup.updateGroup.class})
    private String roleName;

    @Schema(description = "角色描述")
    @Size(max = 255, message = "角色描述长度不能超过255", groups = {RoleGroup.insertGroup.class, RoleGroup.updateGroup.class})
    private String roleDesc;

    @Schema(description = "角色范围")
    @Size(max = 50, message = "角色范围长度不能超过50", groups = {RoleGroup.insertGroup.class, RoleGroup.updateGroup.class})
    private String roleScope;

    @Schema(description = "角色类型")
    @Size(max = 50, message = "角色类型长度不能超过50", groups = {RoleGroup.insertGroup.class, RoleGroup.updateGroup.class})
    private String roleType;

    @Schema(description = "是否删除")
    private Integer isDelete;

    @Schema(description = "角色登录跳转")
    @Size(max = 255, message = "角色登录跳转长度不能超过255", groups = {RoleGroup.insertGroup.class, RoleGroup.updateGroup.class})
    private String skipUrl;
}
