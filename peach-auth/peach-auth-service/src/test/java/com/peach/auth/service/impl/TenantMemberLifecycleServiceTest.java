package com.peach.auth.service.impl;

import com.peach.auth.dao.TenantInvitationDao;
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

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        memberDao = mock(TenantMemberDao.class);
        invitationDao = mock(TenantInvitationDao.class);
        businessIdGenerator = mock(AuthBusinessIdGenerator.class);
        provider = mock(ObjectProvider.class);
        policy = mock(TenantMembershipPolicy.class);
        when(provider.getIfAvailable()).thenReturn(policy);
        service = new TenantMemberLifecycleService(memberDao, invitationDao,
                businessIdGenerator, provider);
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
        verify(memberDao, never()).insert(org.mockito.ArgumentMatchers.any());
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
        when(invitationDao.accept("invitation", "tenant", "user", 1L)).thenReturn(1);
        assertThat(service.accept("tenant", "invitation", "user")).isEqualTo("existing-member");
        verify(memberDao).restore("existing-member", "tenant", 3L);
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
    void shouldNotPermitAdministratorToDisableOwnMembership() {
        assertThatThrownBy(() -> service.disable("tenant", "same", "same"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("own membership");
        verifyNoInteractions(memberDao);
    }

    private static TenantMemberDO member(String id, String status, int deleted, long version) {
        TenantMemberDO member = new TenantMemberDO();
        member.setId(id);
        member.setStatus(status);
        member.setIsDelete(deleted);
        member.setVersion(version);
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
