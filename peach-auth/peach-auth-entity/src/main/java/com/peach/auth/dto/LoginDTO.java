package com.peach.auth.dto;

import java.io.Serial;

import com.peach.auth.group.LoginGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * 登录DTO。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/24 15:36
 */
@Data
@Schema(description = "登录DTO")
public class LoginDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -8673490179027899291L;

    @Schema(description = "用户名，可以是手机号、邮箱、账号")
    @NotBlank(message = "用户名不能为空", groups = {LoginGroup.Login.class})
    @Size(min = 5, max = 20, message = "用户名长度为5-20个字符")
    private String username;

    @Schema(description = "密码")
    @NotBlank(message = "密码不能为空", groups = {LoginGroup.Login.class})
    @Size(max = 1024, message = "密码密文长度不能超过1024个字符", groups = {LoginGroup.Login.class})
    private String password;

    @Schema(description = "年度")
    @NotNull(message = "年度不能为空", groups = {LoginGroup.Login.class})
    private Integer fiscal;

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "机构ID")
    private String orgId;

    @Schema(description = "滑块验证码二次校验凭证，启用验证码时须先通过 /auth/checkCaptcha 获取")
    @Size(max = 2048, message = "验证码凭证长度不能超过2048个字符", groups = {LoginGroup.Login.class})
    private String captchaVerification;

    @Schema(description = "验证码客户端标识，与 getCaptcha/checkCaptcha 保持一致")
    @Size(max = 128, message = "clientUid 长度不能超过128个字符", groups = {LoginGroup.Login.class})
    private String clientUid;
}
