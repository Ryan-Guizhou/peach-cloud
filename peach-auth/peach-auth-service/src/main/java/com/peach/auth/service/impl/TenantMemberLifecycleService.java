package com.peach.auth.service.impl;

import com.peach.auth.dao.OrganizationHierarchyDao;
import com.peach.auth.dao.TenantInvitationDao;
import com.peach.auth.dao.ScopedRoleBindingDao;
import com.peach.auth.dao.MemberAuthVersionDao;
import com.peach.auth.dao.IamAuthorizationAuditDao;
import com.peach.auth.dao.TenantMemberDao;
import com.peach.auth.entity.TenantInvitationDO;
import com.peach.auth.entity.TenantMemberDO;
import com.peach.auth.service.TenantMembershipPolicy;
import com.peach.auth.service.support.AuthBusinessIdGenerator;
import com.peach.auth.service.support.AuthBusinessIdType;
import com.peach.common.unique.UniqueIdFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * SaaS 租户成员及邀请的受控生命周期实现。
 *
 * <p>成员表通过用户与租户物理外键关联，成员退出后恢复原关系而不复用 MEMBER_ID。
 * 所有变更都必须获得可信身份或管理员权限策略授权；缺失策略时默认拒绝。
 * 本服务不暴露 REST 接口，登录流程只允许在验证账号凭据后查询成员列表。
 * 成员状态变更必须同事务撤销原角色绑定、递增授权版本并写入审计；
 * REST 和跨服务网关安全边界由后续 IAM 任务统一对接。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 16:40
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantMemberLifecycleService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";
    private static final String STATUS_LEFT = "LEFT";
    private static final String INVITATION_ACCEPTED = "ACCEPTED";

    private final TenantMemberDao tenantMemberDao;
    private final TenantInvitationDao tenantInvitationDao;
    private final AuthBusinessIdGenerator businessIdGenerator;
    private final ObjectProvider<TenantMembershipPolicy> policyProvider;

    private final ScopedRoleBindingDao scopedRoleBindingDao;

    private final MemberAuthVersionDao authVersionDao;

    private final IamAuthorizationAuditDao authorizationAuditDao;

    private final OrganizationHierarchyDao hierarchyDao;

    /**
     * 供已完成全局账号验证的内部登录流程查询允许选择的租户成员。
     * 不允许直接将该方法暴露给未经认证的 HTTP 请求。
     *
     * @param verifiedUserRefId 已通过服务端凭据校验的用户物理 ID
     * @return 仅包含启用租户与有效用户的 ACTIVE 成员记录
     */
    @Transactional(readOnly = true)
    public List<TenantMemberDO> listForVerifiedUser(String verifiedUserRefId) {
        requireId(verifiedUserRefId, "verifiedUserRefId");
        List<TenantMemberDO> members = tenantMemberDao.selectActiveByUser(verifiedUserRefId);
        return members == null ? Collections.emptyList() : members;
    }

    /**
     * 验证全局用户在指定启用租户的成员资格；机构关系不是必要条件。
     *
     * @param tenantRefId 租户物理 ID
     * @param verifiedUserRefId 已通过服务端凭据校验的用户物理 ID
     * @return 有效成员或 null
     */
    @Transactional(readOnly = true)
    public TenantMemberDO resolveActive(String tenantRefId, String verifiedUserRefId) {
        requireId(tenantRefId, "tenantRefId");
        requireId(verifiedUserRefId, "verifiedUserRefId");
        if (tenantMemberDao.countEnabledTenant(tenantRefId) != 1
                || tenantMemberDao.countEnabledUser(verifiedUserRefId) != 1) {
            return null;
        }
        TenantMemberDO member = tenantMemberDao.selectByUserAndTenant(tenantRefId, verifiedUserRefId);
        return isActive(member) ? member : null;
    }

    /**
     * 创建具备活动唯一槽位的邀请；只有通过服务端授权的租户管理员可操作。
     * 过期邀请被原地转为 EXPIRED，保留历史邀请号。
     *
     * @param tenantRefId 目标租户物理 ID
     * @param operatorMemberRefId 操作者成员物理 ID
     * @param inviteeUserRefId 受邀用户物理 ID
     * @param expiresAt 截止时间，最终以数据库时间校验
     * @return 新邀请物理 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public String invite(String tenantRefId, String operatorMemberRefId,
                         String inviteeUserRefId, LocalDateTime expiresAt) {
        requireId(tenantRefId, "tenantRefId");
        requireId(operatorMemberRefId, "operatorMemberRefId");
        requireId(inviteeUserRefId, "inviteeUserRefId");
        policy().requireTenantAdministrator(tenantRefId, operatorMemberRefId);
        requireActiveTenantAndUser(tenantRefId, inviteeUserRefId);
        if (expiresAt == null || !expiresAt.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Invitation expiry must be in the future");
        }
        TenantMemberDO operator = tenantMemberDao.selectByIdInTenant(operatorMemberRefId, tenantRefId);
        if (!isActive(operator)) {
            throw new SecurityException("Inviter is not an active tenant member");
        }
        TenantMemberDO existing = tenantMemberDao.selectByUserAndTenant(tenantRefId, inviteeUserRefId);
        if (existing != null && !STATUS_LEFT.equals(existing.getStatus())) {
            throw new IllegalStateException("User already has a non-left membership");
        }

        tenantInvitationDao.expirePendingForUser(tenantRefId, inviteeUserRefId);
        TenantInvitationDO invitation = new TenantInvitationDO();
        invitation.setId(UniqueIdFacade.nextId());
        invitation.setInvitationId(businessIdGenerator.next(AuthBusinessIdType.INVITATION));
        invitation.setTenantRefId(tenantRefId);
        invitation.setInviterMemberRefId(operatorMemberRefId);
        invitation.setInviteeUserRefId(inviteeUserRefId);
        invitation.setStatus(STATUS_PENDING);
        invitation.setExpiresAt(expiresAt);
        invitation.fillCreateTime(operatorMemberRefId);
        if (tenantInvitationDao.insert(invitation) != 1) {
            throw new IllegalStateException("Failed to create tenant invitation");
        }
        log.info("Tenant invitation created, tenantRefId={}, invitationRefId={}",
                tenantRefId, invitation.getId());
        return invitation.getId();
    }

    /**
     * 经身份策略校验后接受本人邀请；邀请消费与成员创建/恢复在同一事务提交。
     *
     * @param tenantRefId 目标租户物理 ID
     * @param invitationRefId 邀请物理 ID
     * @param verifiedUserRefId 可信登录用户物理 ID
     * @return 原有或新创建的成员物理 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public String accept(String tenantRefId, String invitationRefId, String verifiedUserRefId) {
        requireId(tenantRefId, "tenantRefId");
        requireId(invitationRefId, "invitationRefId");
        requireId(verifiedUserRefId, "verifiedUserRefId");
        policy().requireCurrentUser(verifiedUserRefId);
        requireActiveTenantAndUser(tenantRefId, verifiedUserRefId);

        TenantInvitationDO invitation = tenantInvitationDao.selectByIdInTenant(invitationRefId, tenantRefId);
        if (invitation == null || !verifiedUserRefId.equals(invitation.getInviteeUserRefId())) {
            throw new SecurityException("Invitation does not belong to current user");
        }
        TenantMemberDO member = tenantMemberDao.selectByUserAndTenant(tenantRefId, verifiedUserRefId);
        if (INVITATION_ACCEPTED.equals(invitation.getStatus()) && isActive(member)) {
            return member.getId();
        }
        if (!STATUS_PENDING.equals(invitation.getStatus())
                || invitation.getIsDelete() == null || invitation.getIsDelete() != 0
                || invitation.getExpiresAt() == null
                || !invitation.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("Invitation is no longer active");
        }

        String memberRefId;
        if (member == null) {
            TenantMemberDO created = new TenantMemberDO();
            created.setId(UniqueIdFacade.nextId());
            created.setMemberId(businessIdGenerator.next(AuthBusinessIdType.MEMBER));
            created.setTenantRefId(tenantRefId);
            created.setUserRefId(verifiedUserRefId);
            created.setStatus(STATUS_ACTIVE);
            created.fillCreateTime(verifiedUserRefId);
            if (tenantMemberDao.insert(created) != 1) {
                throw new IllegalStateException("Failed to create tenant membership");
            }
            memberRefId = created.getId();
        } else if (isActive(member)) {
            memberRefId = member.getId();
        } else if (STATUS_LEFT.equals(member.getStatus()) && Integer.valueOf(1).equals(member.getIsDelete())) {
            if (tenantMemberDao.restore(member.getId(), tenantRefId, requireVersion(member.getVersion())) != 1) {
                throw new IllegalStateException("Concurrent membership restoration");
            }
            invalidateMemberAuthorizations(tenantRefId, member, member.getId(), "MEMBER_REJOIN");
            memberRefId = member.getId();
        } else {
            throw new IllegalStateException("Membership is disabled or inconsistent");
        }
        if (tenantInvitationDao.accept(invitationRefId, tenantRefId,
                verifiedUserRefId, requireVersion(invitation.getVersion())) != 1) {
            throw new IllegalStateException("Invitation was concurrently modified");
        }
        // 邀请与成员状态在同一事务提交，新增关系保留完整加入审计。
        if (authorizationAuditDao.append(UniqueIdFacade.nextId(), tenantRefId, null,
                memberRefId, memberRefId, "MEMBER_JOIN", 0L, 0L) != 1) {
            throw new IllegalStateException("Membership acceptance audit append failed");
        }
        log.info("Tenant invitation accepted, tenantRefId={}, memberRefId={}", tenantRefId, memberRefId);
        return memberRefId;
    }

    /**
     * 当前用户主动退出租户；重复退出为幂等操作。
     *
     * @param tenantRefId 租户物理 ID
     * @param verifiedUserRefId 可信用户物理 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void leave(String tenantRefId, String verifiedUserRefId) {
        requireId(tenantRefId, "tenantRefId");
        requireId(verifiedUserRefId, "verifiedUserRefId");
        policy().requireCurrentUser(verifiedUserRefId);
        lockTenantForProtectedMembershipChange(tenantRefId);
        TenantMemberDO member = tenantMemberDao.selectByUserAndTenant(tenantRefId, verifiedUserRefId);
        if (member == null) {
            throw new IllegalStateException("No tenant membership exists");
        }
        if (STATUS_LEFT.equals(member.getStatus()) && Integer.valueOf(1).equals(member.getIsDelete())) {
            return;
        }
        if (!isActive(member)) {
            throw new IllegalStateException("Unable to leave tenant membership");
        }
        assertNotProtectedOwner(tenantRefId, member.getId());
        invalidateMemberAuthorizations(tenantRefId, member, member.getId(), "MEMBER_LEFT");
        if (tenantMemberDao.softDelete(member.getId(), tenantRefId,
                requireVersion(member.getVersion()), verifiedUserRefId) != 1) {
            throw new IllegalStateException("Concurrent tenant membership departure");
        }
        log.info("Tenant membership left, tenantRefId={}, memberRefId={}", tenantRefId, member.getId());
    }

    /**
     * 管理员停用成员；保留原成员记录和永久业务 ID。
     *
     * @param tenantRefId 租户物理 ID
     * @param operatorMemberRefId 操作者成员物理 ID
     * @param targetMemberRefId 目标成员物理 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void disable(String tenantRefId, String operatorMemberRefId, String targetMemberRefId) {
        changeManagedStatus(tenantRefId, operatorMemberRefId, targetMemberRefId, STATUS_DISABLED);
    }

    /**
     * 管理员恢复已停用成员；主动退出的成员只能再次接受邀请。
     *
     * @param tenantRefId 租户物理 ID
     * @param operatorMemberRefId 操作者成员物理 ID
     * @param targetMemberRefId 目标成员物理 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void enable(String tenantRefId, String operatorMemberRefId, String targetMemberRefId) {
        changeManagedStatus(tenantRefId, operatorMemberRefId, targetMemberRefId, STATUS_ACTIVE);
    }

    /**
     * 管理员取消未接受的邀请。
     *
     * @param tenantRefId 租户物理 ID
     * @param operatorMemberRefId 操作者成员物理 ID
     * @param invitationRefId 邀请物理 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(String tenantRefId, String operatorMemberRefId, String invitationRefId) {
        requireId(tenantRefId, "tenantRefId");
        requireId(operatorMemberRefId, "operatorMemberRefId");
        requireId(invitationRefId, "invitationRefId");
        policy().requireTenantAdministrator(tenantRefId, operatorMemberRefId);
        TenantInvitationDO invitation = tenantInvitationDao.selectByIdInTenant(invitationRefId, tenantRefId);
        if (invitation == null || !STATUS_PENDING.equals(invitation.getStatus())) {
            throw new IllegalStateException("Invitation is not pending");
        }
        if (tenantInvitationDao.cancel(invitationRefId, tenantRefId,
                requireVersion(invitation.getVersion()), operatorMemberRefId) != 1) {
            throw new IllegalStateException("Concurrent invitation cancellation");
        }
    }

    private void changeManagedStatus(String tenantRefId, String operatorMemberRefId,
                                     String targetMemberRefId, String requestedStatus) {
        requireId(tenantRefId, "tenantRefId");
        requireId(operatorMemberRefId, "operatorMemberRefId");
        requireId(targetMemberRefId, "targetMemberRefId");
        policy().requireTenantAdministrator(tenantRefId, operatorMemberRefId);
        lockTenantForProtectedMembershipChange(tenantRefId);
        policy().requireTenantAdministrator(tenantRefId, operatorMemberRefId);
        if (operatorMemberRefId.equals(targetMemberRefId)) {
            throw new IllegalStateException("Administrator cannot change own membership status");
        }
        if (tenantMemberDao.countEnabledTenant(tenantRefId) != 1) {
            throw new IllegalStateException("Tenant is not enabled");
        }
        TenantMemberDO member = tenantMemberDao.selectByIdInTenant(targetMemberRefId, tenantRefId);
        if (member == null || !Integer.valueOf(0).equals(member.getIsDelete())) {
            throw new IllegalStateException("Target membership is not active");
        }
        if (requestedStatus.equals(member.getStatus())) {
            return;
        }
        if (!STATUS_ACTIVE.equals(member.getStatus()) && !STATUS_DISABLED.equals(member.getStatus())) {
            throw new IllegalStateException("Unsupported membership state transition");
        }
        assertNotProtectedOwner(tenantRefId, targetMemberRefId);
        String operation = STATUS_DISABLED.equals(requestedStatus) ? "MEMBER_DISABLED" : "MEMBER_ENABLED";
        invalidateMemberAuthorizations(tenantRefId, member, operatorMemberRefId, operation);
        if (tenantMemberDao.changeStatus(targetMemberRefId, tenantRefId, requestedStatus,
                requireVersion(member.getVersion()), operatorMemberRefId,
                TenantMemberDO.getCurrentTime()) != 1) {
            throw new IllegalStateException("Concurrent member state modification");
        }
        log.info("Tenant membership status changed, tenantRefId={}, memberRefId={}, status={}",
                tenantRefId, targetMemberRefId, requestedStatus);
    }

    /**
     * 同事务清空旧角色绑定、递增成员授权版本并记审计。
     * 停用后恢复原成员记录不会复活任一历史角色绑定。
     *
     * @param tenantRefId 物理租户主键
     * @param member 成员的旧状态快照
     * @param operatorMemberRefId 当前操作人的成员物理主键
     * @param operation 状态变更语义码
     */
    private void lockTenantForProtectedMembershipChange(String tenantRefId) {
        if (!StringUtils.hasText(hierarchyDao.lockTenantAndFindBusinessId(tenantRefId))) {
            throw new SecurityException("Tenant membership target is unavailable");
        }
    }

    private void assertNotProtectedOwner(String tenantRefId, String memberRefId) {
        if (scopedRoleBindingDao.countProtectedOwnerMember(tenantRefId, memberRefId) > 0) {
            throw new SecurityException("Tenant owner cannot be disabled or leave without protected transfer");
        }
    }

    private void invalidateMemberAuthorizations(String tenantRefId, TenantMemberDO member,
                                                String operatorMemberRefId, String operation) {
        long priorVersion = requireVersion(member.getAuthVersion());
        if (priorVersion == Long.MAX_VALUE) {
            throw new IllegalStateException("Member authorization version exhausted");
        }
        scopedRoleBindingDao.revokeAllForMember(tenantRefId, member.getId());
        if (authVersionDao.incrementForMember(tenantRefId, member.getId()) != 1) {
            throw new IllegalStateException("Unable to revoke prior member authorization");
        }
        if (authorizationAuditDao.append(UniqueIdFacade.nextId(), tenantRefId, null,
                operatorMemberRefId, member.getId(), operation, priorVersion, priorVersion + 1) != 1) {
            throw new IllegalStateException("Failed to audit member authorization invalidation");
        }
    }

    private void requireActiveTenantAndUser(String tenantRefId, String userRefId) {
        if (tenantMemberDao.countEnabledTenant(tenantRefId) != 1
                || tenantMemberDao.countEnabledUser(userRefId) != 1) {
            throw new IllegalStateException("Tenant or user is not enabled");
        }
    }

    private TenantMembershipPolicy policy() {
        TenantMembershipPolicy policy = policyProvider.getIfAvailable();
        if (policy == null) {
            throw new SecurityException("Trusted tenant membership authorization policy is unavailable");
        }
        return policy;
    }

    private static boolean isActive(TenantMemberDO member) {
        return member != null && Integer.valueOf(0).equals(member.getIsDelete())
                && STATUS_ACTIVE.equals(member.getStatus());
    }

    private static long requireVersion(Long version) {
        if (version == null || version < 0) {
            throw new IllegalStateException("Membership version is invalid");
        }
        return version;
    }

    private static void requireId(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
