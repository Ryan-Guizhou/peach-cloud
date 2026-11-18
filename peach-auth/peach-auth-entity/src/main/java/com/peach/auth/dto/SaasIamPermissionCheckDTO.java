package com.peach.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 服务端 API 权限事实查询请求。
 *
 * <p>权限码必须是明确的 METHOD:/path；不能包含用户、租户、年度、
 * 任意 SQL 条件或客户端声称的角色范围。查询仅代表 API 入口动作权限，
 * 数据对象还必须由业务服务独立执行行级和字段级授权。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 11:20
 */
@Data
@Schema(description = "可信服务端 API 动作权限查询")
public class SaasIamPermissionCheckDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 202611180901L;

    @NotBlank(message = "API 权限码不能为空")
    @Size(max = 128, message = "API 权限码不能超过128字符")
    private String permissionCode;
}
