package com.peach.auth.service.impl;

import com.peach.auth.dao.TenantInvitationDao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 受邀账号必须在确认自身邀请后接受，且每次立即清理临时全局身份。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 19:45
 */
@ExtendWith(MockitoExtension.class)
class AuthenticatedInvitationServiceTest {

    @Mock
    private AuthInvitationTokenSession tokenSession;
    @Mock
    private TenantInvitationDao invitationDao;
    @Mock
    private TenantMemberLifecycleService lifecycleService;
    @InjectMocks
    private AuthenticatedInvitationService service;

    @Test
    void shouldAcceptOnlyOwnedInvitationAndCloseTemporarySubject() {
        when(invitationDao.findTenantForVerifiedInvitee("invitation", "user-ref"))
                .thenReturn("tenant-ref");

        service.acceptAfterAuthentication("USER_00001", "user-ref", "invitation");

        InOrder ordered = inOrder(tokenSession, invitationDao, lifecycleService);
        ordered.verify(tokenSession).open("USER_00001");
        ordered.verify(invitationDao).findTenantForVerifiedInvitee("invitation", "user-ref");
        ordered.verify(lifecycleService).accept("tenant-ref", "invitation", "user-ref");
        ordered.verify(tokenSession).close();
    }

    @Test
    void shouldRejectAnotherUsersInvitationAndCloseTemporarySubject() {
        assertThatThrownBy(() -> service.acceptAfterAuthentication(
                "USER_00001", "user-ref", "foreign-invitation"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("does not belong");
        verify(tokenSession).close();
        verifyNoInteractions(lifecycleService);
    }

    @Test
    void shouldRejectUnverifiedIdentityBeforeCreatingAnyToken() {
        assertThatThrownBy(() -> service.acceptAfterAuthentication(
                "", "user-ref", "invitation"))
                .isInstanceOf(SecurityException.class);
        verifyNoInteractions(tokenSession, invitationDao, lifecycleService);
    }
}
