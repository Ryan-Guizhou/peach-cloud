package com.peach.auth.rest.internal;

import com.peach.auth.service.IMenuService;
import com.peach.common.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.web.bind.annotation.PostMapping;
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
@RequestMapping("/auth/menu")
@Tag(name = "MenuController", description = "菜单管理")
public class MenuController {

    @Resource
    private IMenuService menuService;

    @Operation(summary = "查询菜单列表")
    @PostMapping("/query")
    public Response query() {
        return Response.success();
    }
}
