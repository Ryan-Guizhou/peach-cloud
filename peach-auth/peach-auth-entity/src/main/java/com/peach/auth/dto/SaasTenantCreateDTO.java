package com.peach.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 平台管理员开通新租户的受控参数。
 *
 * <p>操作者、来源租户、应用和所有管理权限仅由服务端可信会话确定。
 * ownerUserRefId 是待授权的目标账号，而非客户端可指定的操作人。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 18:30
 */
@Data
@Schema(description = "受控租户开通参数")
public class SaasTenantCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 219001L;

    @NotBlank(message = "租户编码不能为空")
    @Size(max = 64, message = "租户编码不能超过64个字符")
    private String tenantCode;

    @NotBlank(message = "租户名称不能为空")
    @Size(max = 128, message = "租户名称不能超过128个字符")
    private String tenantName;

    @NotBlank(message = "应用名称不能为空")
    @Size(max = 64, message = "应用名称不能超过64个字符")
    private String appName;

    @NotBlank(message = "所有者用户物理ID不能为空")
    @Size(max = 32, message = "所有者用户物理ID不能超过32个字符")
    private String ownerUserRefId;
}
