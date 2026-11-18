package com.peach.auth.rest.internal;

import com.peach.auth.dao.TenantInvitationDao;
import com.peach.auth.dto.SaasMemberInviteDTO;
import com.peach.auth.service.impl.TenantMemberLifecycleService;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 管理接口不得接受自报租户或以邀请 ID 冒领跨租户成员资格。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 19:15
 */
@ExtendWith(MockitoExtension.class)
class SaasTenantMemberControllerTest {

    @Mock
    private TenantSessionService sessionService;
    @Mock
    private TenantMemberLifecycleService lifecycleService;
    @Mock
    private TenantInvitationDao invitationDao;
    @InjectMocks
    private SaasTenantMemberController controller;

    @Test
    void shouldUseOnlyAuthenticatedTenantAndOperatorToInvite() {
        when(sessionService.requireCurrent()).thenReturn(context());
        SaasMemberInviteDTO dto = new SaasMemberInviteDTO();
        dto.setInviteeUserRefId("invitee");
        dto.setExpiresAt(LocalDateTime.now().plusDays(2));
        controller.invite(dto);
        verify(lifecycleService).invite("tenant", "member", "invitee", dto.getExpiresAt());
    }

    @Test
    void shouldDenyAnInvitationThatIsNotForCurrentAuthenticatedUser() {
        when(sessionService.requireCurrent()).thenReturn(context());
        assertThatThrownBy(() -> controller.acceptInvitation("invitation"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("does not belong");
        verify(invitationDao).findTenantForVerifiedInvitee("invitation", "user");
        verifyNoInteractions(lifecycleService);
    }

    @Test
    void shouldDeriveTargetTenantFromInvitationOwnedByCurrentUser() {
        when(sessionService.requireCurrent()).thenReturn(context());
        when(invitationDao.findTenantForVerifiedInvitee("invitation", "user"))
                .thenReturn("other-tenant");
        controller.acceptInvitation("invitation");
        verify(lifecycleService).accept("other-tenant", "invitation", "user");
    }

    private static TenantSessionContext context() {
        return new TenantSessionContext("user", "tenant", "member", "app", 2L, 7L);
    }
}
