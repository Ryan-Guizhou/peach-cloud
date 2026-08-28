package com.peach.auth.dto;

import java.io.Serial;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * 个人资料更新参数。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
@Data
@Schema(description = "个人资料更新参数")
public class UserProfileUpdateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -2319857262756514426L;

    @Schema(description = "用户名称")
    @NotBlank(message = "用户名称不能为空")
    @Size(max = 50, message = "用户名称长度不能超过50")
    private String userName;

    @Schema(description = "手机号")
    @Size(max = 20, message = "手机号长度不能超过20")
    private String mobilePhone;

    @Schema(description = "邮箱")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100")
    private String email;
}
