package com.peach.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/18 15:56
 */
@Data
@Schema(description = "角色DTO")
public class RoleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "角色ID")
    private String roleId;

    @Schema(description = "角色编码")
    private String roleCode;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色描述")
    private String roleDesc;

    @Schema(description = "角色范围")
    private String roleScope;

    @Schema(description = "角色类型")
    private String roleType;

    @Schema(description = "是否删除")
    private Integer isDelete;

    @Schema(description = "角色登陆跳转")
    private String skipUrl;

}
