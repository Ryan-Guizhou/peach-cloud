package com.peach.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.peach.auth.common.RsaPasswordUtil;
import com.peach.auth.dao.TenantInvitationDao;
import com.peach.auth.dao.TenantMemberDao;
import com.peach.auth.dao.UserDao;
import com.peach.auth.dto.LoginDTO;
import com.peach.auth.service.LoginLockService;
import com.peach.auth.service.SaasLoginSnapshotService;
import com.peach.auth.service.support.LoginInitConfigSupport;
import com.peach.auth.service.support.PasswordHashSupport;
import com.peach.auth.vo.LoginLockStatusVO;
import com.peach.auth.vo.LoginPermissionSnapshotVO;
import com.peach.auth.vo.LoginTargetVO;
import com.peach.auth.vo.UserVO;
import com.peach.captcha.service.CaptchaService;
import com.peach.common.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 首次受邀账号经过原有密码和验证码安全流程后才可接受租户邀请。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 19:45
 */
@ExtendWith(MockitoExtension.class)
class UserServiceInvitationLoginTest {

    @Mock
    private UserDao userDao;
    @Mock
    private TenantMemberDao tenantMemberDao;
    @Mock
    private TenantInvitationDao invitationDao;
    @Mock
    private TenantMemberLifecycleService lifecycleService;
    @Mock
    private TenantSessionService sessionService;
    @Mock
    private SaasLoginSnapshotService snapshotService;
    @Mock
    private CaptchaService captchaService;
    @Mock
    private LoginLockService loginLockService;
    @Mock
    private LoginInitConfigSupport loginInitConfigSupport;
    @Mock
    private PasswordHashSupport passwordHashSupport;
    @Mock
    private com.peach.auth.service.support.AuthBusinessIdGenerator generator;
    @InjectMocks
    private UserServiceImpl service;

    @Test
    void shouldConsumeOwnedInvitationOnlyAfterPasswordVerificationAndBindFreshTenantToken() {
        UserVO user = globalUser();
        when(userDao.login("member01")).thenReturn(user);
        when(passwordHashSupport.matches("secret", "hashed")).thenReturn(true);
        when(loginLockService.checkLock("member01")).thenReturn(new LoginLockStatusVO());
        when(invitationDao.findTenantForVerifiedInvitee("invite", "user-ref"))
                .thenReturn("tenant-ref");
        when(tenantMemberDao.selectLoginTargets("user-ref")).thenReturn(List.of(target()));
        when(sessionService.bindAfterAuthentication("user-ref", "tenant-ref", "app-ref"))
                .thenReturn(new TenantSessionContext(
                        "user-ref", "tenant-ref", "member-ref", "app-ref", 1L, 1L));
        when(snapshotService.requireSnapshot(any(TenantSessionContext.class)))
                .thenReturn(new LoginPermissionSnapshotVO());

        try (MockedStatic<RsaPasswordUtil> rsa = mockStatic(RsaPasswordUtil.class);
             MockedStatic<StpUtil> token = mockStatic(StpUtil.class)) {
            rsa.when(() -> RsaPasswordUtil.decrypt("encrypted")).thenReturn("secret");
            token.when(StpUtil::getTokenValue).thenReturn("token-for-member");

            Response result = service.login(loginRequest());

            assertThat(result.isSuccess()).isTrue();
            verify(lifecycleService).accept("tenant-ref", "invite", "user-ref");
            token.verify(StpUtil::logout);
            verify(sessionService).bindAfterAuthentication("user-ref", "tenant-ref", "app-ref");
        }
    }

    @Test
    void shouldNotConsumeInvitationWhenPasswordIsInvalid() {
        when(loginLockService.checkLock("member01")).thenReturn(new LoginLockStatusVO());
        when(userDao.login("member01")).thenReturn(globalUser());
        when(passwordHashSupport.matches("wrong", "hashed")).thenReturn(false);
        try (MockedStatic<RsaPasswordUtil> rsa = mockStatic(RsaPasswordUtil.class);
             MockedStatic<StpUtil> token = mockStatic(StpUtil.class)) {
            rsa.when(() -> RsaPasswordUtil.decrypt("encrypted")).thenReturn("wrong");

            Response result = service.login(loginRequest());

            assertThat(result.isSuccess()).isFalse();
            verifyNoInteractions(invitationDao, lifecycleService);
            token.verifyNoInteractions();
        }
    }

    private static LoginDTO loginRequest() {
        LoginDTO request = new LoginDTO();
        request.setUsername("member01");
        request.setPassword("encrypted");
        request.setInvitationRefId("invite");
        return request;
    }

    private static UserVO globalUser() {
        UserVO user = new UserVO();
        user.setId("user-ref");
        user.setUserId("USER_00001");
        user.setUserName("Invitee");
        user.setPassword("hashed");
        return user;
    }

    private static LoginTargetVO target() {
        LoginTargetVO target = new LoginTargetVO();
        target.setTenantRefId("tenant-ref");
        target.setAppRefId("app-ref");
        target.setTenantId("TENANT_00001");
        target.setAppId("APP_00001");
        target.setTenantName("Acme");
        target.setAppName("Workspace");
        return target;
    }
}
