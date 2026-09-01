package com.peach.auth.rest.internal;

import com.peach.common.PeachGroup;

import lombok.RequiredArgsConstructor;

import com.github.pagehelper.PageInfo;
import com.peach.auth.annoation.UserOperLog;
import com.peach.auth.dto.MenuDTO;
import com.peach.auth.enums.UserLogEnum;
import com.peach.auth.qo.MenuQO;
import com.peach.auth.service.IMenuService;
import com.peach.auth.vo.MenuVO;
import com.peach.common.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotBlank;

/**
 * 菜单管理。
 * <p>提供菜单列表查询、单条查询以及基础增删改入口。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/2/1 15:07
 */
@Slf4j
@Indexed
@Validated
@RestController
@RequestMapping("/auth/menu")
@Tag(name = "MenuController", description = "菜单管理")
@RequiredArgsConstructor
public class MenuController {

    private final IMenuService menuService;

    @PostMapping("/pageList")
    @Operation(summary = "分页查询菜单")
    public Response pageList(@RequestBody MenuQO menuQO) {
        PageInfo<MenuVO> pageInfo = menuService.pageList(menuQO);
        return Response.success(pageInfo);
    }

    @GetMapping("/selectById")
    @Operation(summary = "根据菜单ID查询菜单")
    public Response selectById(@NotBlank(message = "菜单ID不能为空") String menuId) {
        MenuVO menuVO = menuService.selectById(menuId);
        return Response.success(menuVO);
    }

    @PostMapping("/add")
    @Operation(summary = "新增菜单")
    @UserOperLog(moduleCode = UserLogEnum.Module.USERSERVICE, optType = UserLogEnum.OptType.INSERT,
            optLevel = UserLogEnum.LogLevel.INFO, optContent = "'新增菜单信息,菜单信息:['+#p0+']'")
    public Response add(@Validated({PeachGroup.InsertGroup.class}) @RequestBody MenuDTO menuDTO) {
        menuService.add(menuDTO);
        return Response.success();
    }

    @DeleteMapping("/delById")
    @Operation(summary = "根据菜单ID删除菜单")
    @UserOperLog(moduleCode = UserLogEnum.Module.USERSERVICE, optType = UserLogEnum.OptType.DELETE,
            optLevel = UserLogEnum.LogLevel.ERROR, optContent = "'删除菜单信息,菜单ID:['+#p0+']'")
    public Response delById(@NotBlank(message = "菜单ID不能为空") String menuId) {
        menuService.delById(menuId);
        return Response.success();
    }

    @PostMapping("/update")
    @Operation(summary = "更新菜单")
    @UserOperLog(moduleCode = UserLogEnum.Module.USERSERVICE, optType = UserLogEnum.OptType.UPDATE,
            optLevel = UserLogEnum.LogLevel.DEBUG, optContent = "'更新菜单信息,菜单信息:['+#p0+']'")
    public Response update(@Validated({PeachGroup.UpdateGroup.class}) @RequestBody MenuDTO menuDTO) {
        menuService.update(menuDTO);
        return Response.success();
    }
}
