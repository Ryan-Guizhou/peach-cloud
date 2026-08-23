package com.peach.auth.rest.internal;

import lombok.RequiredArgsConstructor;

import com.github.pagehelper.PageInfo;
import com.peach.auth.dto.RoleResourceAuthDTO;
import com.peach.auth.qo.AuthResourceQO;
import com.peach.auth.service.IAuthResourceService;
import com.peach.auth.vo.AuthResourceVO;
import com.peach.common.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Indexed;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 角色资源授权接口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 21:10
 */
@Indexed
@Validated
@RestController
@RequestMapping("/auth/authResource")
@Tag(name = "AuthResourceController", description = "角色资源授权")
@RequiredArgsConstructor
public class AuthResourceController {

        private final IAuthResourceService authResourceService;

    @PostMapping("/pageList")
    @Operation(summary = "分页查询角色资源授权")
    public Response pageList(@RequestBody AuthResourceQO authResourceQO) {
        PageInfo<AuthResourceVO> pageInfo = authResourceService.pageList(authResourceQO);
        return Response.success(pageInfo);
    }

    @PostMapping("/list")
    @Operation(summary = "查询角色资源授权")
    public Response list(@RequestBody AuthResourceQO authResourceQO) {
        return Response.success(authResourceService.list(authResourceQO));
    }

    @PostMapping("/saveRoleResources")
    @Operation(summary = "保存角色资源授权")
    public Response saveRoleResources(@Validated @RequestBody RoleResourceAuthDTO roleResourceAuthDTO) {
        authResourceService.saveRoleResources(roleResourceAuthDTO);
        return Response.success();
    }
}
