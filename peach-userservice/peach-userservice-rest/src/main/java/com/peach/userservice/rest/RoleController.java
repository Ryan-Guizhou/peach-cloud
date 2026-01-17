package com.peach.userservice.rest;

import com.github.pagehelper.PageInfo;
import com.peach.common.response.Response;
import com.peach.userservice.qo.RoleQO;
import com.peach.userservice.service.IRoleService;
import com.peach.userservice.vo.RoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:21
 */
@Slf4j
@Indexed
@RestController
@RequestMapping("/user/role")
@Tag(name = "RoleController", description = "角色管理")
public class RoleController {

    @Resource
    private IRoleService roleService;

    @PostMapping("/pageList")
    @Operation(summary = "分页查询角色")
    public Response pageList(@RequestBody RoleQO roleQO) {
        PageInfo<RoleVO> pageInfo = roleService.pageList(roleQO);
        return Response.success(pageInfo);
    }

    @GetMapping("/{roleId}")
    @Operation(summary = "根据角色ID查询角色")
    public Response selectById(@Parameter(required = true, description = "角色ID")
                               @PathVariable("roleId") String roleId) {

        RoleVO roleVO = roleService.selectById(roleId);
        return Response.success(roleVO);
    }
}
