package com.peach.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 当前租户及应用的新角色创建参数，身份范围不接受浏览器指定。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 18:25
 */
@Data
@Schema(description = "租户角色创建")
public class SaasRoleCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -264785645202461390L;

    @NotBlank(message = "角色代码不能为空")
    @Size(max = 20, message = "角色代码不能超过20个字符")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64, message = "角色名称不能超过64个字符")
    private String roleName;
}
