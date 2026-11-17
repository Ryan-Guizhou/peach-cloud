package com.peach.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 替换当前年度角色的全部有效权限物理 ID。
 *
 * <p>空列表表示清空角色权限，null 不是合法的更新参数。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 18:25
 */
@Data
@Schema(description = "租户角色权限差量同步")
public class SaasRolePermissionUpdateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -715072824013377157L;

    @NotNull(message = "权限ID列表不能为空")
    @Size(max = 200, message = "单次不能设置超过200个权限")
    private List<String> permissionRefIds;
}
