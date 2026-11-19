package com.peach.auth.service;

import com.peach.auth.dao.IamAuthorizationAuditDao;
import com.peach.auth.dao.UserDao;
import com.peach.auth.dto.SaasPasswordChangeDTO;
import com.peach.auth.entity.UserDO;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.auth.service.support.PasswordCipherService;
import com.peach.auth.service.support.PasswordHashSupport;
import com.peach.auth.service.support.UserTokenRevoker;
import com.peach.auth.vo.LoginLockStatusVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 全局密码修改必须原子增加凭证版本和审计，并在提交后回收所有设备 Token。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/19 11:10
 */
@ExtendWith(MockitoExtension.class)
class SaasPasswordLifecycleServiceTest {

    private static final String VALID_NEW_PASSWORD = "OtherSecure@2026";

    @Mock
    private TenantSessionService sessionService;
    @Mock
    private UserDao userDao;
    @Mock
    private LoginLockService loginLockService;
    @Mock
    private PasswordCipherService passwordCipherService;
    @Mock
    private PasswordHashSupport passwordHashSupport;
    @Mock
    private UserTokenRevoker tokenRevoker;
    @Mock
    private IamAuthorizationAuditDao auditDao;
    @InjectMocks
    private SaasPasswordLifecycleService service;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void shouldRejectOldPasswordAndRateLimitWithoutWriting() {
        allowContext();
        when(passwordHashSupport.matches("OldSecure@2026", "bcrypt-old")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(request()))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("verification");
        verify(loginLockService).recordFailure("password-change:user");
        verify(userDao, never()).changePassword(anyString(), anyString(), anyLong(), anyString());
        verifyNoInteractions(tokenRevoker, auditDao);
    }

    @Test
    void shouldCommitNewHashAndVersionedAuditBeforeRevokingTokens() {
        allowContext();
        when(passwordHashSupport.matches("OldSecure@2026", "bcrypt-old")).thenReturn(true);
        when(passwordHashSupport.matches(VALID_NEW_PASSWORD, "bcrypt-old")).thenReturn(false);
        when(passwordHashSupport.encode(VALID_NEW_PASSWORD)).thenReturn("bcrypt-new");
        when(userDao.changePassword("user", "bcrypt-new", 4L, "member")).thenReturn(1);
        when(auditDao.append(anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyLong(), anyLong())).thenReturn(1);

        service.changePassword(request());

        verify(userDao).changePassword("user", "bcrypt-new", 4L, "member");
        verify(auditDao).append(anyString(), org.mockito.ArgumentMatchers.eq("tenant"),
                org.mockito.ArgumentMatchers.eq("app"),
                org.mockito.ArgumentMatchers.eq("member"),
                org.mockito.ArgumentMatchers.eq("user"),
                org.mockito.ArgumentMatchers.eq("USER_PASSWORD_CHANGE"),
                org.mockito.ArgumentMatchers.eq(4L), org.mockito.ArgumentMatchers.eq(5L));
        verifyNoInteractions(tokenRevoker);
        for (TransactionSynchronization callback : TransactionSynchronizationManager.getSynchronizations()) {
            callback.afterCommit();
        }
        verify(tokenRevoker).revokeAll("USER_00000001");
    }

    @Test
    void shouldNotRevokeTokensWhenAuditFailsAndTransactionRollsBack() {
        allowContext();
        when(passwordHashSupport.matches("OldSecure@2026", "bcrypt-old")).thenReturn(true);
        when(passwordHashSupport.matches(VALID_NEW_PASSWORD, "bcrypt-old")).thenReturn(false);
        when(passwordHashSupport.encode(VALID_NEW_PASSWORD)).thenReturn("bcrypt-new");
        when(userDao.changePassword("user", "bcrypt-new", 4L, "member")).thenReturn(1);

        assertThatThrownBy(() -> service.changePassword(request()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("audit");
        verifyNoInteractions(tokenRevoker);
    }

    @Test
    void shouldRemainCommittedWhenRedisTokenRevocationFails() {
        allowContext();
        when(passwordHashSupport.matches("OldSecure@2026", "bcrypt-old")).thenReturn(true);
        when(passwordHashSupport.matches(VALID_NEW_PASSWORD, "bcrypt-old")).thenReturn(false);
        when(passwordHashSupport.encode(VALID_NEW_PASSWORD)).thenReturn("bcrypt-new");
        when(userDao.changePassword("user", "bcrypt-new", 4L, "member")).thenReturn(1);
        when(auditDao.append(anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyLong(), anyLong())).thenReturn(1);
        doThrow(new IllegalStateException("redis unavailable")).when(tokenRevoker)
                .revokeAll("USER_00000001");

        service.changePassword(request());
        for (TransactionSynchronization callback : TransactionSynchronizationManager.getSynchronizations()) {
            callback.afterCommit();
        }
        verify(tokenRevoker).revokeAll("USER_00000001");
    }

    @Test
    void shouldRejectWeakPasswordWithoutUpdatingCredentials() {
        allowContext();
        when(passwordCipherService.decrypt("newCipher")).thenReturn("weak");
        when(passwordHashSupport.matches("OldSecure@2026", "bcrypt-old")).thenReturn(true);
        assertThatThrownBy(() -> service.changePassword(request()))
                .isInstanceOf(IllegalArgumentException.class);
        verify(userDao, never()).changePassword(anyString(), anyString(), anyLong(), anyString());
        verifyNoInteractions(tokenRevoker);
    }

    @Test
    void shouldRejectMissingTransactionBeforeDecryptingOrUpdating() {
        TransactionSynchronizationManager.clearSynchronization();
        when(sessionService.requireCurrent()).thenReturn(context());
        assertThatThrownBy(() -> service.changePassword(request()))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(passwordCipherService, userDao, tokenRevoker);
        TransactionSynchronizationManager.initSynchronization();
    }

    @Test
    void shouldRejectConcurrentPasswordChange() {
        allowContext();
        when(passwordHashSupport.matches("OldSecure@2026", "bcrypt-old")).thenReturn(true);
        when(passwordHashSupport.matches(VALID_NEW_PASSWORD, "bcrypt-old")).thenReturn(false);
        when(passwordHashSupport.encode(VALID_NEW_PASSWORD)).thenReturn("bcrypt-new");

        assertThatThrownBy(() -> service.changePassword(request()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Concurrent");
        verifyNoInteractions(tokenRevoker, auditDao);
    }

    private void allowContext() {
        when(sessionService.requireCurrent()).thenReturn(context());
        when(loginLockService.checkLock("password-change:user")).thenReturn(new LoginLockStatusVO());
        when(passwordCipherService.decrypt("oldCipher")).thenReturn("OldSecure@2026");
        when(passwordCipherService.decrypt("newCipher")).thenReturn(VALID_NEW_PASSWORD);
        when(userDao.lockCredential("user")).thenReturn(credential());
    }

    private static SaasPasswordChangeDTO request() {
        SaasPasswordChangeDTO dto = new SaasPasswordChangeDTO();
        dto.setOldPassword("oldCipher");
        dto.setNewPassword("newCipher");
        return dto;
    }

    private static TenantSessionContext context() {
        return new TenantSessionContext("user", "tenant", "member", "app", 2L, 7L);
    }

    private static UserDO credential() {
        UserDO user = new UserDO();
        user.setId("user");
        user.setUserId("USER_00000001");
        user.setPassword("bcrypt-old");
        user.setPasswordVersion(4L);
        return user;
    }
}
