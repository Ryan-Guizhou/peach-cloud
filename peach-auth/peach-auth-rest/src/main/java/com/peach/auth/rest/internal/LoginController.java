package com.peach.auth.rest.internal;

import lombok.RequiredArgsConstructor;

import cn.dev33.satoken.stp.StpUtil;
import com.peach.auth.common.RsaPasswordUtil;
import com.peach.auth.dto.RegisterDTO;
import com.peach.auth.dto.SwitchContextDTO;
import com.peach.auth.service.IUserService;
import com.peach.common.response.Response;
import com.peach.auth.dto.LoginDTO;
import com.peach.auth.group.LoginGroup;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Indexed;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

/**
 * 用户登录。
 * <p>负责登录、注册和登出等认证入口，不承载业务域数据维护逻辑。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/24 15:17
 */
@Indexed
@RestController
@RequestMapping("/auth")
@Tag(name = "LoginController", description = "用户登录")
@RequiredArgsConstructor
public class LoginController {


        private final IUserService userService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Response login(@RequestBody @Validated(LoginGroup.Login.class) LoginDTO loginDTO) {
        Response response = userService.login(loginDTO);
        if (response.isSuccess()) {
            response.setMsg("登录成功");
        }
        return response;
    }

    @Operation(summary = "切换当前租户机构上下文")
    @PostMapping("/switchContext")
    public Response switchContext(@RequestBody @Validated SwitchContextDTO switchContextDTO) {
        return userService.switchContext(switchContextDTO);
    }

    @Operation(summary = "获取登录密码加密公钥")
    @GetMapping("/rsa-public-key")
    public Response rsaPublicKey() {
        return Response.success(RsaPasswordUtil.getPublicKeyBase64());
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    public Response logout() {
        StpUtil.logout();
        Response success = Response.success();
        success.setMsg("登出成功");
        return success;
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Response register(@Parameter(description = "注册信息", required = true) @RequestBody RegisterDTO registerDTO) {
        return userService.register(registerDTO);
    }

    @PostMapping("/forget")
    @Operation(summary = "用户忘记密码")
    public Response forget() {
        return Response.success();
    }

    @PostMapping("/reset")
    @Operation(summary = "用户重置密码")
    public Response reset() {
        return Response.success();
    }

    @PostMapping("/changePassword")
    @Operation(summary = "用户修改密码")
    public Response changePassword() {
        return Response.success();
    }

    @PostMapping("/changeInfo")
    @Operation(summary = "用户修改信息")
    public Response changeInfo() {
        return Response.success();
    }

    @PostMapping("/changeAvatar")
    @Operation(summary = "用户修改头像")
    public Response changeAvatar() {
        return Response.success();
    }

    @PostMapping("/init")
    @Operation(summary = "初始化系统配置")
    public Response init() {
        return Response.success(userService.initLogin());
    }

    @PostMapping("/getCaptcha")
    @Operation(summary = "获取验证码")
    public Response getCaptcha() {
        return Response.success();
    }

    @PostMapping("/checkCaptcha")
    @Operation(summary = "验证验证码")
    public Response checkCaptcha() {
        return Response.success();
    }


}
