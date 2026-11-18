package com.peach.auth.service;

import com.peach.auth.dao.ScopedRoleGrantDao;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.auth.vo.RoleScopeGrantVO;
import com.peach.auth.vo.SaasIamContextVO;
import com.peach.auth.vo.SaasIamPermissionDecisionVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyBoolean;

/**
 * 跨服务权限只能从最终用户 Token 的受信物理身份查询，失败默认拒绝。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 11:35
 */
class SaasIamRemoteAuthorizationServiceTest {

    private TenantSessionService sessionService;
    private ScopedRoleGrantDao grantDao;
    private SaasIamRemoteAuthorizationService service;

    @BeforeEach
    void setUp() {
        sessionService = mock(TenantSessionService.class);
        grantDao = mock(ScopedRoleGrantDao.class);
        service = new SaasIamRemoteAuthorizationService(sessionService, grantDao);
        when(sessionService.requireForToken("real-user-token")).thenReturn(
                new TenantSessionContext("user", "tenant", "member", "app", 2026, 2L, 7L));
    }

    @Test
    void shouldQueryApiPermissionWithinVerifiedTenantMemberApplicationAndYear() {
        when(grantDao.selectVisibleApiCodes("tenant", "app", "GET"))
                .thenReturn(List.of("GET:/api/documents"));
        when(grantDao.selectApiGrants("tenant", "member", "app", 2026,
                "GET:/api/documents", false)).thenReturn(List.of(new RoleScopeGrantVO()));
        SaasIamPermissionDecisionVO decision =
                service.decideApiPermission("real-user-token", "GET:/api/documents");
        assertThat(decision.isAllowed()).isTrue();
        assertThat(decision.getContext().getMemberRefId()).isEqualTo("member");
        assertThat(decision.getContext().getAuthVersion()).isEqualTo(7L);
        verify(grantDao).selectApiGrants("tenant", "member", "app", 2026,
                "GET:/api/documents", false);
    }

    @Test
    void shouldDenyPermissionNotFoundWithoutLegacyScopeFallback() {
        when(grantDao.selectVisibleApiCodes("tenant", "app", "DELETE"))
                .thenReturn(List.of("DELETE:/api/documents"));
        assertThat(service.hasApiPermission("real-user-token", "DELETE:/api/documents")).isFalse();
        verify(grantDao).selectApiGrants("tenant", "member", "app", 2026,
                "DELETE:/api/documents", false);
    }

    @Test
    void shouldRejectUnknownApiCodesAndKeepDatabaseUnqueried() {
        assertThat(service.hasApiPermission("real-user-token", "TENANT.ROLE.MANAGE")).isFalse();
        assertThat(service.hasApiPermission("real-user-token", "GET:/api/../admin")).isFalse();
        assertThat(service.hasApiPermission("real-user-token", "GET:/api/hello?admin=1")).isFalse();
        assertThat(service.hasApiPermission("real-user-token", "GET:/api/**")).isFalse();
        assertThat(service.hasApiPermission("real-user-token", "GET:/api/%2e%2e/admin")).isFalse();
        assertThat(service.hasApiPermission("real-user-token", "")).isFalse();
        verifyNoInteractions(grantDao);
    }

    @Test
    void shouldDenyInvalidOrRevokedTokenRatherThanFallingBackToClientValues() {
        when(sessionService.requireForToken("revoked")).thenThrow(new SecurityException("Token revoked"));
        assertThatThrownBy(() -> service.hasApiPermission("revoked", "GET:/api/documents"))
                .isInstanceOf(SecurityException.class);
        verifyNoInteractions(grantDao);
    }

    @Test
    void shouldAuthorizeOnlyMostSpecificRegisteredApiTemplate() {
        when(grantDao.selectVisibleApiCodes("tenant", "app", "GET"))
                .thenReturn(List.of("GET:/api/users/{id}", "GET:/api/users/secret"));
        when(grantDao.selectApiGrants("tenant", "member", "app", 2026,
                "GET:/api/users/{id}", false)).thenReturn(List.of(new RoleScopeGrantVO()));

        assertThat(service.hasApiPermission("real-user-token", "GET:/api/users/123")).isTrue();
        assertThat(service.hasApiPermission("real-user-token", "GET:/api/users/secret")).isFalse();
        verify(grantDao).selectApiGrants("tenant", "member", "app", 2026,
                "GET:/api/users/secret", false);
    }

    @Test
    void shouldDenyIncompleteOrAmbiguousCatalogWithoutGrantLookup() {
        when(grantDao.selectVisibleApiCodes("tenant", "app", "GET"))
                .thenReturn(List.of("GET:/api/{kind}/preview", "GET:/api/files/{id}"));
        assertThat(service.hasApiPermission("real-user-token", "GET:/api/files/preview")).isFalse();
        verify(grantDao, never()).selectApiGrants(
                anyString(), anyString(), anyString(), anyInt(), anyString(),
                anyBoolean());
    }

    @Test
    void shouldRequireSystemOwnedPermissionForProtectedAuthRoutes() {
        when(grantDao.selectVisibleApiCodes("tenant", "app", "POST"))
                .thenReturn(List.of("POST:/api/auth/saas/roles"));
        when(grantDao.selectApiGrants("tenant", "member", "app", 2026,
                "POST:/api/auth/saas/roles", true)).thenReturn(List.of(new RoleScopeGrantVO()));

        assertThat(service.hasApiPermission("real-user-token",
                "POST:/api/auth/saas/roles")).isTrue();
        verify(grantDao).selectApiGrants("tenant", "member", "app", 2026,
                "POST:/api/auth/saas/roles", true);
    }

    @Test
    void shouldNotUseTenantOwnedGrantsForProtectedAuthApi() {
        when(grantDao.selectVisibleApiCodes("tenant", "app", "POST"))
                .thenReturn(List.of("POST:/api/auth/saas/roles"));
        assertThat(service.hasApiPermission("real-user-token",
                "POST:/api/auth/saas/roles")).isFalse();
        verify(grantDao).selectApiGrants("tenant", "member", "app", 2026,
                "POST:/api/auth/saas/roles", true);
    }

    @Test
    void shouldExposeOnlyVerifiedPhysicalIdentityAndVersions() {
        SaasIamContextVO result = service.requireContext("real-user-token");
        assertThat(result.getUserRefId()).isEqualTo("user");
        assertThat(result.getTenantRefId()).isEqualTo("tenant");
        assertThat(result.getMemberRefId()).isEqualTo("member");
        assertThat(result.getAppRefId()).isEqualTo("app");
        assertThat(result.getYear()).isEqualTo(2026);
        assertThat(result.getMemberVersion()).isEqualTo(2L);
        assertThat(result.getAuthVersion()).isEqualTo(7L);
        verify(grantDao, never()).selectApiGrants(
                anyString(), anyString(), anyString(), anyInt(), anyString());
    }
}
