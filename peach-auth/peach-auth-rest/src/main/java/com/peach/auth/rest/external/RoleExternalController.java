package com.peach.auth.rest.external;

import lombok.RequiredArgsConstructor;

import com.peach.auth.service.IRoleService;
import com.peach.common.response.Response;
import com.peach.auth.vo.RoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/18 17:33
 */
@Slf4j
@Indexed
@RestController
@RequestMapping("/auth/external")
@Tag(name = "角色管理外部接口")
@RequiredArgsConstructor
public class RoleExternalController {

        private final IRoleService roleService;

    @Operation(summary = "根据角色ID查询角色信息")
    @GetMapping("/role/{roleId}")
    public Response selectById(@PathVariable("roleId") String roleId) {
        RoleVO roleVO = roleService.selectById(roleId);
        return Response.success(roleVO);
    }
}
