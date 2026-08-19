package com.peach.auth.rest.internal;

import com.github.pagehelper.PageInfo;
import com.peach.auth.dto.RoleFunctionAuthDTO;
import com.peach.auth.qo.AuthFunctionQO;
import com.peach.auth.service.IAuthFunctionService;
import com.peach.auth.vo.AuthFunctionVO;
import com.peach.common.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Indexed;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

/**
 * 角色功能授权接口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 21:10
 */
@Indexed
@Validated
@RestController
@RequestMapping("/auth/authFunction")
@Tag(name = "AuthFunctionController", description = "角色功能授权")
public class AuthFunctionController {

    @Resource
    private IAuthFunctionService authFunctionService;

    @PostMapping("/pageList")
    @Operation(summary = "分页查询角色功能授权")
    public Response pageList(@RequestBody AuthFunctionQO authFunctionQO) {
        PageInfo<AuthFunctionVO> pageInfo = authFunctionService.pageList(authFunctionQO);
        return Response.success(pageInfo);
    }

    @PostMapping("/list")
    @Operation(summary = "查询角色功能授权")
    public Response list(@RequestBody AuthFunctionQO authFunctionQO) {
        return Response.success(authFunctionService.list(authFunctionQO));
    }

    @PostMapping("/saveRoleFunctions")
    @Operation(summary = "保存角色功能授权")
    public Response saveRoleFunctions(@Validated @RequestBody RoleFunctionAuthDTO roleFunctionAuthDTO) {
        authFunctionService.saveRoleFunctions(roleFunctionAuthDTO);
        return Response.success();
    }
}
