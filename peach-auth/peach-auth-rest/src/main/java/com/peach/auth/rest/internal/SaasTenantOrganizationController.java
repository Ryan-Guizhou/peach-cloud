package com.peach.auth.rest.internal;

import com.peach.auth.dto.SaasOrganizationCreateDTO;
import com.peach.auth.dto.SaasOrganizationMoveDTO;
import com.peach.auth.dto.SaasOrganizationUpdateDTO;
import com.peach.auth.service.SaasOrganizationManagementService;
import com.peach.auth.service.impl.OrganizationHierarchyService;
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
 * 使用实时 SaaS 成员上下文管理机构，拒绝外部指定的租户和操作者。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 19:05
 */
@Indexed
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/saas/tenants/organizations")
@Tag(name = "SaasTenantOrganizationController", description = "SaaS租户机构树管理")
public class SaasTenantOrganizationController {

    private final TenantSessionService sessionService;
    private final SaasOrganizationManagementService organizationService;
    private final OrganizationHierarchyService hierarchyService;

    @PostMapping
    @Operation(summary = "在当前租户创建机构")
    public Response create(@Valid @RequestBody SaasOrganizationCreateDTO request) {
        return Response.success(organizationService.create(request));
    }

    @PutMapping("/{orgRefId}")
    @Operation(summary = "修改当前租户机构名称")
    public Response rename(@NotBlank @PathVariable String orgRefId,
                           @Valid @RequestBody SaasOrganizationUpdateDTO request) {
        organizationService.rename(orgRefId, request.getOrgName());
        return Response.success();
    }

    @DeleteMapping("/{orgRefId}")
    @Operation(summary = "经依赖检查后逻辑删除机构")
    public Response retire(@NotBlank @PathVariable String orgRefId) {
        organizationService.retire(orgRefId);
        return Response.success();
    }

    @PostMapping("/{orgRefId}/move")
    @Operation(summary = "移动机构并阻止跨租户和环形父节点")
    public Response move(@NotBlank @PathVariable String orgRefId,
                         @Valid @RequestBody SaasOrganizationMoveDTO request) {
        TenantSessionContext actor = sessionService.requireCurrent();
        hierarchyService.move(actor.tenantRefId(), actor.memberRefId(),
                orgRefId, request.getNewParentOrgRefId());
        return Response.success();
    }
}
