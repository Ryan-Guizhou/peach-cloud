package com.peach.auth.rest.internal;

import com.peach.auth.dto.SaasMemberRoleGrantDTO;
import com.peach.auth.dto.SaasRoleCreateDTO;
import com.peach.auth.dto.SaasRolePermissionUpdateDTO;
import com.peach.auth.service.ScopedRoleBindingService;
import com.peach.auth.service.ScopedRoleManagementService;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.common.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Indexed;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SaaS 租户角色及成员角色绑定的受保护 REST 入口。
 *
 * <p>客户端仅提交业务操作参数，不接受操作者、租户、应用身份声明。
 * 所有操作从当前 Token Session 重新验证物理成员，领域服务还需逐项
 * 检查完整的租户权限与可委派权限，并使用事务处理版本失效和审计。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 18:25
 */
@Indexed
@Validated
@RestController
@RequestMapping("/auth/saas/roles")
@Tag(name = "SaasRoleController", description = "SaaS角色及成员授权")
@RequiredArgsConstructor
public class SaasRoleController {

    private final TenantSessionService tenantSessionService;

    private final ScopedRoleManagementService roleManagementService;

    private final ScopedRoleBindingService roleBindingService;

    @PostMapping
    @Operation(summary = "创建当前租户、应用和的自定义角色")
    public Response createRole(@Valid @RequestBody SaasRoleCreateDTO request) {
        TenantSessionContext context = tenantSessionService.requireCurrent();
        return Response.success(roleManagementService.createRole(
                context.tenantRefId(), context.memberRefId(),
                request.getRoleCode(), request.getRoleName()));
    }

    @PutMapping("/{roleRefId}/permissions")
    @Operation(summary = "差量同步当前角色的全部权限")
    public Response replacePermissions(@NotBlank @PathVariable String roleRefId,
                                       @Valid @RequestBody SaasRolePermissionUpdateDTO request) {
        TenantSessionContext context = tenantSessionService.requireCurrent();
        roleManagementService.replacePermissions(
                context.tenantRefId(), context.memberRefId(), roleRefId,
                request.getPermissionRefIds());
        return Response.success();
    }

    @PostMapping("/bindings")
    @Operation(summary = "授予成员一个独立的角色和数据作用域")
    public Response grantRole(@Valid @RequestBody SaasMemberRoleGrantDTO request) {
        TenantSessionContext context = tenantSessionService.requireCurrent();
        return Response.success(roleBindingService.grant(
                context.tenantRefId(), context.memberRefId(),
                request.getTargetMemberRefId(), request.getRoleRefId(),
                request.getScopeType(), request.getScopeRefId()));
    }

    @DeleteMapping("/bindings/{bindingRefId}")
    @Operation(summary = "撤销成员的一个角色绑定")
    public Response revokeRole(@NotBlank @PathVariable String bindingRefId) {
        TenantSessionContext context = tenantSessionService.requireCurrent();
        roleBindingService.revoke(context.tenantRefId(), context.memberRefId(), bindingRefId);
        return Response.success();
    }
}
