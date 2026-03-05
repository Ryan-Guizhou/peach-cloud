package com.peach.auth.rest.internal;

import cn.dev33.satoken.stp.StpUtil;
import com.peach.auth.service.IUserService;
import com.peach.common.response.Response;
import com.peach.auth.dto.LoginDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/24 15:17
 */
@Slf4j
@Indexed
@RestController
@RequestMapping("/auth")
@Tag(name = "LoginController", description = "用户登录")
public class LoginController {


    @Resource
    private IUserService userService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Response login(@RequestBody LoginDTO loginDTO) {
        Response response = userService.login(loginDTO);
        response.setMsg("登录成功");
        return response;
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
    public Response register() {
        return Response.success();
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
        return Response.success();
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
