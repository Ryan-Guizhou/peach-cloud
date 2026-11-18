package com.peach.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 受信任的角色可委派权限查询结果。
 *
 * <p>权限物理 ID 和永久语义码必须来自权限目录的同一行。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 18:25
 */
@Data
@Schema(description = "受控角色权限定义")
public class RolePermissionGrantVO implements Serializable {

    @Serial
    private static final long serialVersionUID = -576434929081171755L;

    private String permissionRefId;

    private String permissionCode;

    private String ownerType;
}
