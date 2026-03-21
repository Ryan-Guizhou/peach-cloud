package com.peach.auth.dto;

import com.peach.auth.group.LoginGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/21 20:42
 */
@Data
@Schema(description = "登录DTO")
public class RegisterDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(description = "用户名,可以是手机号、邮箱、账号")
    @NotBlank(message = "用户名不能为空", groups = {LoginGroup.Register.class})
    @Size(min = 5, max = 20, message = "用户名长度为5-20个字符")
    private String username;

    @Schema(description = "密码")
    @NotBlank(message = "确认密码不能为空",groups = {LoginGroup.Register.class})
    @Size(min = 9, max = 20, message = "密码长度为9-20")
    private String password;


    @Schema(description = "密码")
    @NotBlank(message = "确认密码不能为空",groups = {LoginGroup.Register.class})
    @Size(min = 9, max = 20, message = "密码长度为9-20")
    private String confirmPassword;


}
