package com.peach.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * 已认证用户修改全局账号密码的 RSA 密文参数。
 *
 * <p>所有用户、成员与租户身份由服务端 Token 决定，不接受请求指定用户；
 * 原始密码及密文均不得进入日志或对象字符串。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/19 11:00
 */
@Data
@Schema(description = "当前用户安全修改密码")
public class SaasPasswordChangeDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 220001L;

    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Schema(description = "经当前 RSA 公钥加密的原密码")
    @NotBlank(message = "原密码密文不能为空")
    @Size(max = 1024, message = "原密码密文长度不能超过1024个字符")
    private String oldPassword;

    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Schema(description = "经当前 RSA 公钥加密的新密码")
    @NotBlank(message = "新密码密文不能为空")
    @Size(max = 1024, message = "新密码密文长度不能超过1024个字符")
    private String newPassword;
}
