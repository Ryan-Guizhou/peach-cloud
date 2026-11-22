package com.peach.auth.rest.internal;

import com.peach.common.PeachGroup;

import lombok.RequiredArgsConstructor;

import com.github.pagehelper.PageInfo;
import com.peach.log.annoation.UserOperLog;
import com.peach.auth.dto.RoleDTO;
import com.peach.log.enums.UserLogEnum;
import com.peach.auth.qo.RoleQO;
import com.peach.auth.service.IRoleService;
import com.peach.auth.vo.RoleVO;
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
 * 角色管理。
 * <p>提供角色列表查询、单条查询以及基础增删改入口。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/2/1 15:07
 */
@Slf4j
@Indexed
@Validated
@RestController
@RequestMapping("/auth/role")
@Tag(name = "RoleController", description = "角色管理")
@RequiredArgsConstructor
public class RoleController {

    private final IRoleService roleService;
    private final com.peach.auth.service.impl.RoleWorkspaceService workspace;

    @GetMapping("/workspace")
    @Operation(summary = "查询角色功能和资源完整快照")
    public Response workspace(@NotBlank String roleId) { return Response.success(workspace.get(roleId)); }

    @GetMapping("/members")
    @Operation(summary = "查询角色成员和可选用户")
    public Response members(@NotBlank String roleId) { return Response.success(workspace.members(roleId)); }

    @PostMapping("/saveGrants")
    @UserOperLog(optContent = "角色功能与资源授权")
    public Response saveGrants(@jakarta.validation.Valid @RequestBody com.peach.auth.dto.RoleWorkspaceDTO input) { workspace.saveGrants(input); return Response.success(); }

    @PostMapping("/calibrate")
    @Operation(summary = "预览失效或越界授权，不写库")
    public Response calibrate(@jakarta.validation.Valid @RequestBody com.peach.auth.dto.RoleWorkspaceDTO input) { return Response.success(workspace.calibrate(input)); }

    @PostMapping("/saveMembers")
    @UserOperLog(optContent = "角色成员维护")
    public Response saveMembers(@jakarta.validation.Valid @RequestBody com.peach.auth.dto.RoleWorkspaceDTO input) { workspace.saveMembers(input); return Response.success(); }

    @PostMapping("/saveDefault")
    @UserOperLog(optContent = "角色默认菜单设置")
    public Response saveDefault(@jakarta.validation.Valid @RequestBody com.peach.auth.dto.RoleWorkspaceDTO input) { workspace.saveDefault(input); return Response.success(); }

    @PostMapping("/pageList")
    @Operation(summary = "分页查询角色")
    public Response pageList(@RequestBody RoleQO roleQO) {
        PageInfo<RoleVO> pageInfo = roleService.pageList(roleQO);
        return Response.success(pageInfo);
    }

    @GetMapping("/selectById")
    @Operation(summary = "根据角色ID查询角色")
    public Response selectById(@NotBlank(message = "角色ID不能为空") String roleId) {
        RoleVO roleVO = roleService.selectById(roleId);
        return Response.success(roleVO);
    }

    @PostMapping("/add")
    @Operation(summary = "新增角色")
    @UserOperLog(moduleCode = UserLogEnum.Module.USERSERVICE, optType = UserLogEnum.OptType.INSERT,
            optLevel = UserLogEnum.LogLevel.INFO, optContent = "'新增角色信息,角色信息:['+#p0+']'")
    public Response add(@Validated({PeachGroup.InsertGroup.class}) @RequestBody RoleDTO roleDTO) {
        roleService.add(roleDTO);
        return Response.success();
    }

    @DeleteMapping("/delById")
    @Operation(summary = "根据角色ID删除角色")
    @UserOperLog(moduleCode = UserLogEnum.Module.USERSERVICE, optType = UserLogEnum.OptType.DELETE,
            optLevel = UserLogEnum.LogLevel.ERROR, optContent = "'删除角色信息,角色ID:['+#p0+']'")
    public Response delById(@NotBlank(message = "角色ID不能为空") String roleId) {
        roleService.delById(roleId);
        return Response.success();
    }

    @PostMapping("/update")
    @Operation(summary = "更新角色")
    @UserOperLog(moduleCode = UserLogEnum.Module.USERSERVICE, optType = UserLogEnum.OptType.UPDATE,
            optLevel = UserLogEnum.LogLevel.DEBUG, optContent = "'更新角色信息,角色信息:['+#p0+']'")
    public Response update(@Validated({PeachGroup.UpdateGroup.class}) @RequestBody RoleDTO roleDTO) {
        roleService.update(roleDTO);
        return Response.success();
    }
}
