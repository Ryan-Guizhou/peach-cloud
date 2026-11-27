package com.peach.auth.launch;

import com.peach.auth.dao.UserDao;
import com.peach.auth.dao.UserOrgDao;
import com.peach.auth.dto.LoginDTO;
import com.peach.auth.service.IRoleService;
import com.peach.auth.service.LoginLockService;
import com.peach.auth.service.assembler.LoginPermissionAssembler;
import com.peach.auth.service.impl.UserServiceImpl;
import com.peach.auth.service.support.*;
import com.peach.auth.vo.LoginLockStatusVO;
import com.peach.captcha.service.CaptchaService;
import com.peach.auth.common.RsaPasswordUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * 现有认证失败分支的审计原因回归；不改变密码校验契约。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 20:00
 */
@ExtendWith(MockitoExtension.class)
class LoginAuditAuthenticationTest {
    @Mock TenantLoginAvailability availability;
    @Mock UserDao users;
    @Mock UserOrgDao orgs;
    @Mock IRoleService roles;
    @Mock CaptchaService captcha;
    @Mock LoginLockService locks;
    @Mock LoginInitConfigSupport init;
    @Mock LoginPermissionAssembler permissions;
    @Mock LoginPermissionCacheRefresher refresher;
    @InjectMocks UserServiceImpl service;
    LoginDTO request;
    @BeforeEach void begin() {
        request = new LoginDTO(); request.setUsername("audit-account"); request.setPassword("encrypted-fixture");
        LoginAuditContext.begin("LOGIN", "127.0.0.1", "Other", "trace");
    }
    @AfterEach void clear() { LoginAuditContext.clear(); }
    @Test void captchaFailureIsRecordedBeforeAccountLookup() {
        when(init.isCaptchaRequired()).thenReturn(true);
        assertFalse(service.login(request).isSuccess());
        assertEquals("CAPTCHA_FAILED", LoginAuditContext.finish().getReasonCode());
        verifyNoInteractions(users, locks);
    }
    @Test void lockedAccountIsRecordedWithoutCredentialProcessing() {
        var locked = new LoginLockStatusVO(); locked.setLocked(true);
        when(locks.checkLock("audit-account")).thenReturn(locked);
        assertFalse(service.login(request).isSuccess());
        assertEquals("ACCOUNT_LOCKED", LoginAuditContext.finish().getReasonCode());
        verifyNoInteractions(users);
    }
    @Test void failedCredentialLookupDoesNotTrustRequestedTenant() {
        when(locks.checkLock("audit-account")).thenReturn(new LoginLockStatusVO());
        when(locks.recordFailure("audit-account")).thenReturn(new LoginLockStatusVO());
        request.setTenantId("untrusted-requested-tenant");
        try (var loader = mockStatic(com.peach.auth.common.util.InstanceLazyLoader.class)) {
            loader.when(() -> com.peach.auth.common.util.InstanceLazyLoader.getInstance(org.redisson.api.RedissonClient.class))
                    .thenReturn(mock(org.redisson.api.RedissonClient.class));
            try (var rsa = mockStatic(RsaPasswordUtil.class)) {
                rsa.when(() -> RsaPasswordUtil.decrypt(anyString())).thenReturn("fixture-plaintext");
                assertFalse(service.login(request).isSuccess());
            }
        }
        assertEquals("BAD_CREDENTIALS", LoginAuditContext.finish().getReasonCode());
        assertNull(LoginAuditContext.finish().getTenantId());
    }
}
