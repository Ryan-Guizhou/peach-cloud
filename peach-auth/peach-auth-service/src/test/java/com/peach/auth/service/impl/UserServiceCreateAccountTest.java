package com.peach.auth.service.impl;

import com.peach.auth.dao.UserDao;
import com.peach.auth.dto.UserDTO;
import com.peach.auth.entity.UserDO;
import com.peach.auth.service.PlatformAdministrationAuthorizationService;
import com.peach.auth.service.support.AuthBusinessIdGenerator;
import com.peach.auth.service.support.AuthBusinessIdType;
import com.peach.auth.service.support.PasswordHashSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 平台全局账号新增必须具备合法默认状态，且初始密码不以明文持久化。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/20 15:00
 */
@ExtendWith(MockitoExtension.class)
class UserServiceCreateAccountTest {

    @Mock
    private UserDao userDao;

    @Mock
    private AuthBusinessIdGenerator authBusinessIdGenerator;

    @Mock
    private PasswordHashSupport passwordHashSupport;

    @Mock
    private PlatformAdministrationAuthorizationService platformAuthorizationService;

    @InjectMocks
    private UserServiceImpl service;

    @Test
    void shouldCreateEnabledUserWithHashedInitialPassword() {
        when(authBusinessIdGenerator.next(AuthBusinessIdType.USER)).thenReturn("USER-BIZ-1");
        when(passwordHashSupport.encode("FirstPassword123!")).thenReturn("server-bcrypt-hash");
        UserDTO dto = request("FirstPassword123!", null);

        service.add(dto);

        ArgumentCaptor<UserDO> captor = ArgumentCaptor.forClass(UserDO.class);
        verify(userDao).insert(captor.capture());
        UserDO record = captor.getValue();
        assertThat(record.getStatus()).isEqualTo("ENABLE");
        assertThat(record.getPassword()).isEqualTo("server-bcrypt-hash");
        assertThat(record.getPassword()).isNotEqualTo(dto.getPassword());
        assertThat(record.getUserId()).isEqualTo("USER-BIZ-1");
        assertThat(record.getId()).isNotBlank();
        assertThat(record.getIsDelete()).isZero();
        assertThat(record.getIsModify()).isZero();
    }

    @Test
    void shouldRetainExplicitDisabledStatusWithHashedPassword() {
        when(authBusinessIdGenerator.next(AuthBusinessIdType.USER)).thenReturn("USER-BIZ-2");
        when(passwordHashSupport.encode("FirstPassword123!")).thenReturn("server-bcrypt-hash");

        service.add(request("FirstPassword123!", "DISABLED"));

        ArgumentCaptor<UserDO> captor = ArgumentCaptor.forClass(UserDO.class);
        verify(userDao).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("DISABLED");
    }

    @Test
    void shouldRejectUnrecognizedStatusBeforeWritingAccount() {
        when(authBusinessIdGenerator.next(AuthBusinessIdType.USER)).thenReturn("USER-BIZ-3");
        when(passwordHashSupport.encode("FirstPassword123!")).thenReturn("server-bcrypt-hash");

        assertThatThrownBy(() -> service.add(request("FirstPassword123!", "ADMIN")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("status");
        verifyNoInteractions(userDao);
    }

    @Test
    void shouldRefuseGlobalAccountCreationWithoutPlatformPrivilege() {
        doThrow(new SecurityException("PLATFORM administrator required"))
                .when(platformAuthorizationService).requirePlatformAdministrator();

        assertThatThrownBy(() -> service.add(request("FirstPassword123!", null)))
                .isInstanceOf(SecurityException.class);
        verifyNoInteractions(userDao, authBusinessIdGenerator, passwordHashSupport);
    }

    @Test
    void shouldRejectGlobalAccountWithoutLoginCode() {
        UserDTO dto = request("FirstPassword123!", null);
        dto.setUserCode(null);
        assertThatThrownBy(() -> service.add(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("account code");
        verifyNoInteractions(userDao, authBusinessIdGenerator, passwordHashSupport);
    }

    private static UserDTO request(String password, String status) {
        UserDTO dto = new UserDTO();
        dto.setUserCode("account001");
        dto.setUserName("Platform test account");
        dto.setPassword(password);
        dto.setStatus(status);
        return dto;
    }
}
