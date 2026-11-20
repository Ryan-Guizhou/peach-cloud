package com.peach.auth.service.impl;

import com.peach.auth.dao.UserAvatarHistoryDao;
import com.peach.auth.dao.UserDao;
import com.peach.auth.qo.UserQO;
import com.peach.auth.vo.UserProfileVO;
import com.peach.auth.vo.UserVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 个人中心必须使用受信物理用户标识访问本人资料。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/20 12:00
 */
@ExtendWith(MockitoExtension.class)
class UserProfileSaasContextTest {

    private static final String USER_REF_ID = "0123456789abcdef0123456789abcdef";

    @Mock
    private TenantSessionService sessionService;
    @Mock
    private UserDao userDao;
    @Mock
    private UserAvatarHistoryDao avatarDao;
    @InjectMocks
    private UserProfileServiceImpl service;

    @Test
    void shouldRejectRevokedSessionBeforeQueryingUser() {
        when(sessionService.requireCurrent()).thenThrow(new SecurityException("Session revoked"));

        assertThatThrownBy(service::getCurrentProfile).isInstanceOf(SecurityException.class);
        verifyNoInteractions(userDao, avatarDao);
    }

    @Test
    void shouldRejectMissingUserMapping() {
        when(sessionService.requireCurrent()).thenReturn(context());

        assertThatThrownBy(service::getCurrentProfile)
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("identity");
        verify(userDao).findBusinessIdByPhysicalId(USER_REF_ID);
        verify(userDao, never()).selectByQO(any(UserQO.class));
        verifyNoInteractions(avatarDao);
    }

    @Test
    void shouldReadProfileForVerifiedPhysicalUserOnly() {
        when(sessionService.requireCurrent()).thenReturn(context());
        when(userDao.findBusinessIdByPhysicalId(USER_REF_ID)).thenReturn("USER-0001");
        UserVO user = new UserVO();
        user.setUserId("USER-0001");
        user.setUserName("Current user");
        when(userDao.selectByQO(any(UserQO.class))).thenReturn(List.of(user));
        when(avatarDao.selectActiveByUserId("USER-0001")).thenReturn(List.of());

        UserProfileVO profile = service.getCurrentProfile();

        assertThat(profile.getUserId()).isEqualTo("USER-0001");
        ArgumentCaptor<UserQO> query = ArgumentCaptor.forClass(UserQO.class);
        verify(userDao).selectByQO(query.capture());
        assertThat(query.getValue().getUserId()).isEqualTo("USER-0001");
    }

    private static TenantSessionContext context() {
        return new TenantSessionContext(USER_REF_ID, "tenant", "member", "app", 2, 7);
    }
}
