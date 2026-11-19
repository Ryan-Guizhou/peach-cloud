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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 校验租户成员邀请接受、退出和管理员授权的基本安全状态机。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 16:50
 */
class TenantMemberLifecycleServiceTest {

    private TenantMemberDao memberDao;
    private TenantInvitationDao invitationDao;
    private AuthBusinessIdGenerator businessIdGenerator;
    private ObjectProvider<TenantMembershipPolicy> provider;
    private TenantMembershipPolicy policy;
    private TenantMemberLifecycleService service;
    private ScopedRoleBindingDao roleBindingDao;
    private MemberAuthVersionDao memberAuthVersionDao;
    private IamAuthorizationAuditDao auditDao;
    private OrganizationHierarchyDao hierarchyDao;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        memberDao = mock(TenantMemberDao.class);
        invitationDao = mock(TenantInvitationDao.class);
        businessIdGenerator = mock(AuthBusinessIdGenerator.class);
        provider = mock(ObjectProvider.class);
        policy = mock(TenantMembershipPolicy.class);
        when(provider.getIfAvailable()).thenReturn(policy);
        roleBindingDao = mock(ScopedRoleBindingDao.class);
        memberAuthVersionDao = mock(MemberAuthVersionDao.class);
        auditDao = mock(IamAuthorizationAuditDao.class);
        hierarchyDao = mock(OrganizationHierarchyDao.class);
        when(hierarchyDao.lockTenantAndFindBusinessId("tenant")).thenReturn("tenant-business");
        service = new TenantMemberLifecycleService(memberDao, invitationDao,
                businessIdGenerator, provider, roleBindingDao, memberAuthVersionDao, auditDao,
                hierarchyDao);
    }

    @Test
    void shouldFailClosedWithoutTrustedAuthorizationPolicy() {
        when(provider.getIfAvailable()).thenReturn(null);
        assertThatThrownBy(() -> service.leave("tenant", "user"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("unavailable");
        verifyNoInteractions(memberDao, invitationDao, businessIdGenerator);
    }

    @Test
    void shouldReadAvailableTenantMembersWithoutOrganizationMembership() {
        TenantMemberDO member = member("member-id", "ACTIVE", 0, 1L);
        when(memberDao.selectActiveByUser("user")).thenReturn(List.of(member));
        assertThat(service.listForVerifiedUser("user")).containsExactly(member);
        verify(memberDao).selectActiveByUser("user");
    }

    @Test
    void shouldRejectInvitationForDifferentUser() {
        when(memberDao.countEnabledTenant("tenant")).thenReturn(1);
        when(memberDao.countEnabledUser("user")).thenReturn(1);
        TenantInvitationDO invitation = invitation("someone-else", "PENDING");
        when(invitationDao.selectByIdInTenant("invitation", "tenant")).thenReturn(invitation);
        assertThatThrownBy(() -> service.accept("tenant", "invitation", "user"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("does not belong");
        verifyNoInteractions(businessIdGenerator);
        verify(memberDao, never()).insert(any());
    }

    @Test
    void shouldRestoreOriginalMembershipWhenAcceptingASecondInvitation() {
        when(memberDao.countEnabledTenant("tenant")).thenReturn(1);
        when(memberDao.countEnabledUser("user")).thenReturn(1);
        when(invitationDao.selectByIdInTenant("invitation", "tenant"))
                .thenReturn(invitation("user", "PENDING"));
        when(memberDao.selectByUserAndTenant("tenant", "user"))
                .thenReturn(member("existing-member", "LEFT", 1, 3L));
        when(memberDao.restore("existing-member", "tenant", 3L)).thenReturn(1);
        when(memberAuthVersionDao.incrementForMember("tenant", "existing-member")).thenReturn(1);
        when(auditDao.append(anyString(), eq("tenant"), isNull(),
                eq("existing-member"), eq("existing-member"), eq("MEMBER_REJOIN"),
                eq(0L), eq(1L))).thenReturn(1);
        when(invitationDao.accept("invitation", "tenant", "user", 1L)).thenReturn(1);
        when(auditDao.append(anyString(), eq("tenant"), isNull(),
                eq("existing-member"), eq("existing-member"), eq("MEMBER_JOIN"),
                eq(0L), eq(0L))).thenReturn(1);
        assertThat(service.accept("tenant", "invitation", "user")).isEqualTo("existing-member");
        verify(memberDao).restore("existing-member", "tenant", 3L);
        verify(roleBindingDao).revokeAllForMember("tenant", "existing-member");
        verify(memberAuthVersionDao).incrementForMember("tenant", "existing-member");
        verifyNoInteractions(businessIdGenerator);
    }

    @Test
    void shouldReturnExistingMembershipForAlreadyAcceptedInvitation() {
        when(memberDao.countEnabledTenant("tenant")).thenReturn(1);
        when(memberDao.countEnabledUser("user")).thenReturn(1);
        when(invitationDao.selectByIdInTenant("invitation", "tenant"))
                .thenReturn(invitation("user", "ACCEPTED"));
        when(memberDao.selectByUserAndTenant("tenant", "user"))
                .thenReturn(member("existing-member", "ACTIVE", 0, 2L));
        assertThat(service.accept("tenant", "invitation", "user")).isEqualTo("existing-member");
        verifyNoInteractions(businessIdGenerator);
    }

    @Test
    void leavingMustSoftDeleteMembershipAndRevokeAllPriorRoleBindings() {
        when(memberDao.selectByUserAndTenant("tenant", "user"))
                .thenReturn(member("member-id", "ACTIVE", 0, 4L));
        when(memberAuthVersionDao.incrementForMember("tenant", "member-id")).thenReturn(1);
        when(auditDao.append(anyString(), eq("tenant"), isNull(),
                eq("member-id"), eq("member-id"), eq("MEMBER_LEFT"), eq(0L), eq(1L)))
                .thenReturn(1);
        when(memberDao.softDelete("member-id", "tenant", 4L, "user")).thenReturn(1);

        service.leave("tenant", "user");

        verify(roleBindingDao).revokeAllForMember("tenant", "member-id");
        verify(memberAuthVersionDao).incrementForMember("tenant", "member-id");
        verify(memberDao).softDelete("member-id", "tenant", 4L, "user");
    }

    @Test
    void shouldNotPermitAdministratorToDisableOwnMembership() {
        assertThatThrownBy(() -> service.disable("tenant", "same", "same"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("own membership");
        verifyNoInteractions(memberDao);
    }

    @Test
    void shouldDenyLeavingAsLastEffectiveTenantAdministrator() {
        when(memberDao.selectByUserAndTenant("tenant", "user"))
                .thenReturn(member("member-id", "ACTIVE", 0, 1L));
        when(roleBindingDao.countAdministratorMember("tenant", "member-id")).thenReturn(1);
        when(roleBindingDao.countActiveTenantAdministrators("tenant")).thenReturn(1);

        assertThatThrownBy(() -> service.leave("tenant", "user"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Last tenant administrator");
        verify(memberDao, never()).softDelete(anyString(), anyString(),
                org.mockito.ArgumentMatchers.anyLong(), anyString());
        verifyNoInteractions(memberAuthVersionDao);
    }

    @Test
    void shouldDenyDisablingProtectedOwnerEvenWhenAnotherAdminExists() {
        when(memberDao.countEnabledTenant("tenant")).thenReturn(1);
        when(memberDao.selectByIdInTenant("target", "tenant"))
                .thenReturn(member("target", "ACTIVE", 0, 1L));
        when(roleBindingDao.countProtectedOwnerMember("tenant", "target")).thenReturn(1);

        assertThatThrownBy(() -> service.disable("tenant", "admin", "target"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Tenant owner");
        verifyNoInteractions(memberAuthVersionDao);
    }

    @Test
    void shouldFailClosedWhenNewMembershipAcceptanceAuditCannotPersist() {
        when(memberDao.countEnabledTenant("tenant")).thenReturn(1);
        when(memberDao.countEnabledUser("user")).thenReturn(1);
        when(invitationDao.selectByIdInTenant("invitation", "tenant"))
                .thenReturn(invitation("user", "PENDING"));
        when(memberDao.insert(any(TenantMemberDO.class))).thenReturn(1);
        when(invitationDao.accept("invitation", "tenant", "user", 1L)).thenReturn(1);

        assertThatThrownBy(() -> service.accept("tenant", "invitation", "user"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("audit");
    }

    private static TenantMemberDO member(String id, String status, int deleted, long version) {
        TenantMemberDO member = new TenantMemberDO();
        member.setId(id);
        member.setStatus(status);
        member.setIsDelete(deleted);
        member.setVersion(version);
        member.setAuthVersion(0L);
        return member;
    }

    private static TenantInvitationDO invitation(String userRefId, String status) {
        TenantInvitationDO invitation = new TenantInvitationDO();
        invitation.setInviteeUserRefId(userRefId);
        invitation.setStatus(status);
        invitation.setIsDelete(0);
        invitation.setVersion(1L);
        invitation.setExpiresAt(LocalDateTime.now().plusDays(1));
        return invitation;
    }
}
