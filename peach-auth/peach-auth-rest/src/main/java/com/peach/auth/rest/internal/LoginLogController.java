package com.peach.auth.rest.internal;

import com.peach.auth.qo.LoginLogQO;
import com.peach.auth.service.impl.LoginLogService;
import com.peach.common.response.Response;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

/**
 * 登录日志只读入口；服务端强制验证查询权限和租户归属。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 20:00
 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/auth/loginLog")
@Tag(name = "LoginLogController", description = "登录日志")
public class LoginLogController {
    private final LoginLogService service;
    @PostMapping("/pageList")
    @Operation(summary = "分页查询登录日志")
    public Response pageList(@Valid @RequestBody LoginLogQO query) { return Response.success(service.pageList(query)); }
    @GetMapping("/selectById")
    @Operation(summary = "查询登录日志详情")
    public Response selectById(@RequestParam @NotBlank @Size(max = 32) String logId) { return Response.success(service.selectById(logId)); }
}
