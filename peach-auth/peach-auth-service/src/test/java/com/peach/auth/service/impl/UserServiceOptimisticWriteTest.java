package com.peach.auth.service.impl;

import com.peach.auth.dao.UserDao;
import com.peach.auth.dto.UserDTO;
import com.peach.auth.entity.UserDO;
import com.peach.auth.service.PlatformAdministrationAuthorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 全局用户管理必须使用物理 ID 和预期记录版本 CAS，
 * 并由受信当前 SaaS 成员身份承担删除审计。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/20 15:00
 */
@ExtendWith(MockitoExtension.class)
class UserServiceOptimisticWriteTest {

    private static final String USER_REF_ID = "0123456789abcdef0123456789abcdef";

    @Mock
    private UserDao userDao;

    @Mock
    private TenantSessionService sessionService;

    @Mock
    private PlatformAdministrationAuthorizationService platformAuthorizationService;

    @InjectMocks
    private UserServiceImpl service;

    @Test
    void shouldIncrementOneVersionedUserUpdateOnly() {
        UserDTO dto = editable(7L);
        when(userDao.updateAdminWithVersion(any(UserDO.class))).thenReturn(1, 0);

        assertThatCode(() -> service.update(dto)).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.update(dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("concurrently");

        ArgumentCaptor<UserDO> captor = ArgumentCaptor.forClass(UserDO.class);
        verify(userDao, times(2)).updateAdminWithVersion(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(user -> {
            assertThat(user.getId()).isEqualTo(USER_REF_ID);
            assertThat(user.getVersion()).isEqualTo(7L);
            assertThat(user.getUserName()).isEqualTo("Updated name");
        });
    }

    @Test
    void shouldRejectMissingAndNegativeUpdateVersionsBeforeWrite() {
        UserDTO missing = editable(null);
        UserDTO negative = editable(-1L);

        assertThatThrownBy(() -> service.update(missing))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.update(negative))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(userDao);
    }

    @Test
    void shouldRecordTrustedMemberPhysicalIdForVersionedSoftDelete() {
        when(sessionService.requireCurrent()).thenReturn(
                new TenantSessionContext("user", "tenant", "trusted-member", "app", 2, 7));
        when(userDao.softDeleteWithVersion(USER_REF_ID, 3L, "trusted-member"))
                .thenReturn(1, 0);

        assertThatCode(() -> service.delById(USER_REF_ID, 3L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.delById(USER_REF_ID, 3L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("concurrently");
        verify(userDao, times(2)).softDeleteWithVersion(USER_REF_ID, 3L, "trusted-member");
    }

    @Test
    void shouldRejectInvalidDeletionVersionsBeforeQueryingCurrentSession() {
        assertThatThrownBy(() -> service.delById(USER_REF_ID, -1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.delById(" ", 1L))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(userDao, sessionService, platformAuthorizationService);
    }

    @Test
    void shouldNotCallUserDaoWhenPlatformAuthorizationIsMissing() {
        doThrow(new SecurityException("PLATFORM privilege required"))
                .when(platformAuthorizationService).requirePlatformAdministrator();
        assertThatThrownBy(() -> service.update(editable(0L)))
                .isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> service.delById(USER_REF_ID, 0L))
                .isInstanceOf(SecurityException.class);
        verifyNoInteractions(userDao, sessionService);
    }

    private static UserDTO editable(Long version) {
        UserDTO dto = new UserDTO();
        dto.setId(USER_REF_ID);
        dto.setVersion(version);
        dto.setUserName("Updated name");
        return dto;
    }
}
