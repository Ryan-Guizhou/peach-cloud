package com.peach.auth.rest.internal;

import com.peach.auth.dao.TenantInvitationDao;
import com.peach.auth.dto.SaasMemberInviteDTO;
import com.peach.auth.service.impl.TenantMemberLifecycleService;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.common.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Indexed;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 基于真实 SaaS 成员 Token 的邀请和成员状态管理入口。
 *
 * <p>所有管理员操作仅使用当前物理租户和成员，不接受自报来源租户或操作者。
 * 邀请接受通过受邀人身份反查目标租户，不使用客户端传入的 tenantRefId。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 18:45
 */
@Indexed
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/saas/tenants/members")
@Tag(name = "SaasTenantMemberController", description = "可信租户成员与邀请管理")
public class SaasTenantMemberController {

    private final TenantSessionService sessionService;
    private final TenantMemberLifecycleService lifecycleService;
    private final TenantInvitationDao invitationDao;

    @PostMapping("/invitations")
    @Operation(summary = "邀请现有全局用户加入当前租户")
    public Response invite(@Valid @RequestBody SaasMemberInviteDTO request) {
        TenantSessionContext actor = sessionService.requireCurrent();
        return Response.success(lifecycleService.invite(actor.tenantRefId(), actor.memberRefId(),
                request.getInviteeUserRefId(), request.getExpiresAt()));
    }

    @DeleteMapping("/invitations/{invitationRefId}")
    @Operation(summary = "取消当前租户未被接受的邀请")
    public Response cancelInvitation(@NotBlank @PathVariable String invitationRefId) {
        TenantSessionContext actor = sessionService.requireCurrent();
        lifecycleService.cancel(actor.tenantRefId(), actor.memberRefId(), invitationRefId);
        return Response.success();
    }

    @PostMapping("/invitations/{invitationRefId}/accept")
    @Operation(summary = "当前已认证全局用户接受本人邀请")
    public Response acceptInvitation(@NotBlank @PathVariable String invitationRefId) {
        TenantSessionContext actor = sessionService.requireCurrent();
        String targetTenant = invitationDao.findTenantForVerifiedInvitee(
                invitationRefId, actor.userRefId());
        if (!StringUtils.hasText(targetTenant)) {
            throw new SecurityException("Active invitation does not belong to current user");
        }
        return Response.success(lifecycleService.accept(
                targetTenant, invitationRefId, actor.userRefId()));
    }

    @PostMapping("/{memberRefId}/disable")
    @Operation(summary = "停用当前租户的非所有者成员")
    public Response disable(@NotBlank @PathVariable String memberRefId) {
        TenantSessionContext actor = sessionService.requireCurrent();
        lifecycleService.disable(actor.tenantRefId(), actor.memberRefId(), memberRefId);
        return Response.success();
    }

    @PostMapping("/{memberRefId}/enable")
    @Operation(summary = "恢复当前租户的已停用成员")
    public Response enable(@NotBlank @PathVariable String memberRefId) {
        TenantSessionContext actor = sessionService.requireCurrent();
        lifecycleService.enable(actor.tenantRefId(), actor.memberRefId(), memberRefId);
        return Response.success();
    }

    @PostMapping("/leave")
    @Operation(summary = "当前成员退出所在租户，受保护所有者不得直接退出")
    public Response leave() {
        TenantSessionContext actor = sessionService.requireCurrent();
        lifecycleService.leave(actor.tenantRefId(), actor.userRefId());
        return Response.success();
    }
}
