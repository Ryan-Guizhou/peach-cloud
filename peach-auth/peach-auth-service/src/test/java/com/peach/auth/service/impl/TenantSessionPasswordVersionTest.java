package com.peach.auth.service.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.peach.auth.dao.TenantMemberDao;
import com.peach.auth.dao.UserDao;
import com.peach.auth.entity.TenantMemberDO;
import com.peach.common.constant.SaTokenConstant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 跨设备或跨租户旧 Token 必须因全局密码版本递增而即时失效。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/19 11:10
 */
@ExtendWith(MockitoExtension.class)
class TenantSessionPasswordVersionTest {

    @Mock
    private TenantMemberDao tenantMemberDao;
    @Mock
    private TenantMemberLifecycleService membershipService;
    @Mock
    private UserDao userDao;
    @InjectMocks
    private TenantSessionService service;

    @Test
    void shouldRejectTokenWithRevokedGlobalPasswordVersion() {
        SaSession session = tokenSession(3L);
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::isLogin).thenReturn(true);
            stp.when(StpUtil::getTokenSession).thenReturn(session);
            stp.when(StpUtil::getLoginIdAsString).thenReturn("USER_00000001");
            assertThatThrownBy(service::requireCurrent)
                    .isInstanceOf(SecurityException.class);
        }
        verify(userDao).countActivePasswordVersion("user", "USER_00000001", 3L);
        verifyNoInteractions(membershipService);
    }

    @Test
    void shouldRejectLegacyTokenWithoutPasswordVersion() {
        SaSession session = tokenSession(0L);
        Map<String, String> values = tokenValues(0L);
        values.remove("passwordVersion");
        when(session.get(SaTokenConstant.TOKEN_SESSION_SAAS_CONTEXT)).thenReturn(values);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::isLogin).thenReturn(true);
            stp.when(StpUtil::getTokenSession).thenReturn(session);
            stp.when(StpUtil::getLoginIdAsString).thenReturn("USER_00000001");
            assertThatThrownBy(service::requireCurrent).isInstanceOf(SecurityException.class);
        }
        verifyNoInteractions(userDao, membershipService);
    }

    @Test
    void shouldAllowOnlyCurrentPasswordVersionAndActiveTenantMember() {
        SaSession session = tokenSession(4L);
        when(userDao.countActivePasswordVersion("user", "USER_00000001", 4L)).thenReturn(1);
        when(tenantMemberDao.countAuthenticatedUserIdentity("USER_00000001", "user")).thenReturn(1);
        when(tenantMemberDao.countEnabledApplication("tenant", "app")).thenReturn(1);
        TenantMemberDO member = new TenantMemberDO();
        member.setId("member");
        member.setStatus("ACTIVE");
        member.setVersion(2L);
        member.setAuthVersion(7L);
        member.setIsDelete(0);
        when(membershipService.resolveActive("tenant", "user")).thenReturn(member);
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::isLogin).thenReturn(true);
            stp.when(StpUtil::getTokenSession).thenReturn(session);
            stp.when(StpUtil::getLoginIdAsString).thenReturn("USER_00000001");
            assertThat(service.requireCurrent().tenantRefId()).isEqualTo("tenant");
        }
    }

    @Test
    void shouldRejectPasswordChangeBetweenCredentialCheckAndBinding() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::isLogin).thenReturn(true);
            stp.when(StpUtil::getLoginIdAsString).thenReturn("USER_00000001");
            when(tenantMemberDao.countAuthenticatedUserIdentity("USER_00000001", "user"))
                    .thenReturn(1);
            assertThatThrownBy(() -> service.bindAfterAuthentication(
                    "user", "tenant", "app", 2L)).isInstanceOf(SecurityException.class);
        }
        verify(userDao).countActivePasswordVersion("user", "USER_00000001", 2L);
        verifyNoInteractions(membershipService);
    }

    private static SaSession tokenSession(long passwordVersion) {
        SaSession session = mock(SaSession.class);
        when(session.get(SaTokenConstant.TOKEN_SESSION_SAAS_CONTEXT))
                .thenReturn(tokenValues(passwordVersion));
        return session;
    }

    private static Map<String, String> tokenValues(long passwordVersion) {
        Map<String, String> values = new HashMap<>();
        values.put("userRefId", "user");
        values.put("tenantRefId", "tenant");
        values.put("memberRefId", "member");
        values.put("appRefId", "app");
        values.put("memberVersion", "2");
        values.put("authVersion", "7");
        values.put("passwordVersion", String.valueOf(passwordVersion));
        return values;
    }
}
