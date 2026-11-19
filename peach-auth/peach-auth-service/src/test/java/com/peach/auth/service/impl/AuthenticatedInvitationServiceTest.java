package com.peach.auth.service.impl;

import com.peach.auth.dao.TenantInvitationDao;
import com.peach.auth.dao.UserDao;
import com.peach.auth.entity.UserDO;
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
    private UserDao userDao;
    @Mock
    private TenantInvitationDao invitationDao;
    @Mock
    private TenantMemberLifecycleService lifecycleService;
    @InjectMocks
    private AuthenticatedInvitationService service;

    @Test
    void shouldAcceptOnlyOwnedInvitationAndCloseTemporarySubject() {
        when(userDao.lockCredential("user-ref")).thenReturn(credential(4L));
        when(invitationDao.findTenantForVerifiedInvitee("invitation", "user-ref"))
                .thenReturn("tenant-ref");

        service.acceptAfterAuthentication("USER_00001", "user-ref", "invitation", 4L);

        InOrder ordered = inOrder(userDao, tokenSession, invitationDao, lifecycleService);
        ordered.verify(userDao).lockCredential("user-ref");
        ordered.verify(tokenSession).open("USER_00001");
        ordered.verify(invitationDao).findTenantForVerifiedInvitee("invitation", "user-ref");
        ordered.verify(lifecycleService).accept("tenant-ref", "invitation", "user-ref");
        ordered.verify(tokenSession).close();
    }

    @Test
    void shouldRejectAnotherUsersInvitationAndCloseTemporarySubject() {
        when(userDao.lockCredential("user-ref")).thenReturn(credential(4L));
        assertThatThrownBy(() -> service.acceptAfterAuthentication(
                "USER_00001", "user-ref", "foreign-invitation", 4L))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("does not belong");
        verify(tokenSession).close();
        verifyNoInteractions(lifecycleService);
    }

    @Test
    void shouldRejectUnverifiedIdentityBeforeCreatingAnyToken() {
        assertThatThrownBy(() -> service.acceptAfterAuthentication(
                "", "user-ref", "invitation", 4L))
                .isInstanceOf(SecurityException.class);
        verifyNoInteractions(tokenSession, userDao, invitationDao, lifecycleService);
    }

    @Test
    void shouldRejectStalePasswordVersionBeforeCreatingTemporaryToken() {
        when(userDao.lockCredential("user-ref")).thenReturn(credential(5L));
        assertThatThrownBy(() -> service.acceptAfterAuthentication(
                "USER_00001", "user-ref", "invitation", 4L))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Password");
        verifyNoInteractions(tokenSession, invitationDao, lifecycleService);
    }

    private static UserDO credential(long version) {
        UserDO user = new UserDO();
        user.setId("user-ref");
        user.setUserId("USER_00001");
        user.setPasswordVersion(version);
        return user;
    }
}
