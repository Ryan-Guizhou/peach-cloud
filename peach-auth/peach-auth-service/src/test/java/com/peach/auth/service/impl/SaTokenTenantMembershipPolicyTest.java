package com.peach.auth.service.impl;

import com.peach.auth.dao.TenantMemberDao;
import com.peach.auth.service.ScopedPermissionService;
import com.peach.auth.service.support.IamPermissionCodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 租户管理员权限必须与同一 Token 的成员、租户、应用严格绑定。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 18:10
 */
class SaTokenTenantMembershipPolicyTest {

    private TenantSessionService sessionService;
    private ScopedPermissionService permissionService;
    private SaTokenTenantMembershipPolicy policy;

    @BeforeEach
    void setUp() {
        sessionService = mock(TenantSessionService.class);
        permissionService = mock(ScopedPermissionService.class);
        policy = new SaTokenTenantMembershipPolicy(
                mock(TenantMemberDao.class), sessionService, permissionService);
        when(sessionService.requireCurrent()).thenReturn(
                new TenantSessionContext("user", "tenant", "member", "app", 1L, 3L));
    }

    @Test
    void shouldAllowOnlyTheExactTenantScopedAction() {
        when(permissionService.hasTenantPermission(
                "tenant", "member", "app", IamPermissionCodes.TENANT_MEMBER_MANAGE))
                .thenReturn(true);
        assertThatCode(() -> policy.requireTenantAdministrator("tenant", "member"))
                .doesNotThrowAnyException();
        verify(permissionService).hasTenantPermission(
                "tenant", "member", "app", IamPermissionCodes.TENANT_MEMBER_MANAGE);
    }

    @Test
    void shouldRejectWrongTenantOrDifferentOperator() {
        assertThatThrownBy(() -> policy.requireTenantAdministrator("another-tenant", "member"))
                .isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> policy.requireTenantAdministrator("tenant", "other-member"))
                .isInstanceOf(SecurityException.class);
        verifyNoInteractions(permissionService);
    }

    @Test
    void shouldDenyMissingTenantGrantByDefault() {
        assertThatThrownBy(() -> policy.requireTenantAdministrator("tenant", "member"))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void shouldRejectBlankPermissionCode() {
        assertThatThrownBy(() -> policy.requireTenantPermission("tenant", "member", " "))
                .isInstanceOf(SecurityException.class);
    }
}
