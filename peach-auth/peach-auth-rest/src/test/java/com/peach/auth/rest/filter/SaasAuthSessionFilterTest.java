package com.peach.auth.rest.filter;

import cn.dev33.satoken.same.SaSameUtil;
import cn.dev33.satoken.stp.StpUtil;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.satoken.filter.UserContextFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * SaaS 角色管理端点须同时校验服务身份、用户登录态与实时成员授权版本。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 13:30
 */
class SaasAuthSessionFilterTest {

    private TenantSessionService sessionService;
    private SaasAuthSessionFilter filter;

    @BeforeEach
    void setUp() {
        sessionService = mock(TenantSessionService.class);
        filter = new SaasAuthSessionFilter(sessionService);
    }

    @Test
    void shouldApplyOnlyToAuthRoleManagementPaths() {
        assertThat(filter.shouldNotFilter(request("POST", "/auth/saas/roles"))).isFalse();
        assertThat(filter.shouldNotFilter(request("POST", "/auth/switchContext"))).isFalse();
        assertThat(filter.shouldNotFilter(request("POST", "/auth/changePassword"))).isFalse();
        assertThat(filter.shouldNotFilter(request("POST", "/auth/saas/platform/tenants"))).isFalse();
        assertThat(filter.shouldNotFilter(request("PUT", "/auth/saas/code-rules/ORG/format"))).isFalse();
        assertThat(filter.shouldNotFilter(request("POST", "/auth/saas/tenants/members/invitations"))).isFalse();
        assertThat(filter.shouldNotFilter(request("POST", "/auth/organization/pageList"))).isFalse();
        assertThat(filter.shouldNotFilter(request("POST", "/auth/user/pageList"))).isFalse();
        assertThat(filter.shouldNotFilter(request("POST", "/auth/user/add"))).isFalse();
        assertThat(filter.shouldNotFilter(request("POST", "/auth/menu/add"))).isFalse();
        assertThat(filter.shouldNotFilter(request("DELETE", "/auth/function/delById"))).isFalse();
        assertThat(filter.shouldNotFilter(request("POST", "/auth/resource/pageList"))).isFalse();
        assertThat(filter.shouldNotFilter(request("POST", "/auth/router/update"))).isFalse();
        assertThat(filter.shouldNotFilter(request("POST", "/auth/authLog/pageList"))).isFalse();
        assertThat(filter.shouldNotFilter(request("POST", "/auth/operLog/pageList"))).isFalse();
        assertThat(filter.shouldNotFilter(request("GET", "/auth/user/selectById"))).isFalse();
        assertThat(filter.shouldNotFilter(request("PUT",
                "/auth/saas/roles/1234567890abcdef1234567890abcdef/permissions"))).isFalse();
        assertThat(filter.shouldNotFilter(request("POST", "/auth/login"))).isTrue();
        assertThat(filter.shouldNotFilter(request("POST", "/auth/external/iam/check-api"))).isTrue();
        assertThat(filter.shouldNotFilter(request("GET", "/api/files/private"))).isTrue();
    }

    @Test
    void shouldDenyEncodedOrTraversingRoleRoutesBeforeReadingAnyToken() throws Exception {
        assertThat(filter.isCanonicalSessionPath("/auth/saas/roles")).isTrue();
        assertThat(filter.isCanonicalSessionPath("/auth/user/pageList")).isTrue();
        assertThat(filter.isCanonicalSessionPath("/auth/menu/pageList")).isTrue();
        assertThat(filter.isCanonicalSessionPath("/auth/function/add")).isTrue();
        assertThat(filter.isCanonicalSessionPath("/auth/resource/delById")).isTrue();
        assertThat(filter.isCanonicalSessionPath("/auth/router/update")).isTrue();
        assertThat(filter.isCanonicalSessionPath("/auth/authLog/pageList")).isTrue();
        assertThat(filter.isCanonicalSessionPath("/auth/operLog/pageList")).isTrue();
        assertThat(filter.isCanonicalSessionPath("/auth/operLog/%2e%2e/pageList")).isFalse();
        assertThat(filter.isCanonicalSessionPath("/auth/menu/%2e%2e/add")).isFalse();
        assertThat(filter.isCanonicalSessionPath("/auth/function/../add")).isFalse();
        assertThat(filter.isCanonicalSessionPath("/auth/router//add")).isFalse();
        assertThat(filter.isCanonicalSessionPath("/auth/user/%2e%2e/add")).isFalse();
        assertThat(filter.isCanonicalSessionPath("/auth/switchContext")).isTrue();
        assertThat(filter.isCanonicalSessionPath("/auth/changePassword")).isTrue();
        assertThat(filter.isCanonicalSessionPath("/auth/saas/platform/tenants")).isTrue();
        assertThat(filter.isCanonicalSessionPath("/auth/saas/code-rules/ORG/preview")).isTrue();
        assertThat(filter.isCanonicalSessionPath("/auth/saas/code-rules/%2e%2e/format")).isFalse();
        assertThat(filter.isCanonicalSessionPath("/auth/saas/tenants/organizations")).isTrue();
        assertThat(filter.isCanonicalSessionPath("/auth/saas/platform/%2e%2e/roles")).isFalse();
        assertThat(filter.isCanonicalSessionPath("/auth/saas/tenants//members")).isFalse();
        assertThat(filter.isCanonicalSessionPath("/auth/switchContext/%2e%2e")).isFalse();
        assertThat(filter.isCanonicalSessionPath("/auth/saas/roles/%2e%2e/users")).isFalse();
        assertThat(filter.isCanonicalSessionPath("/auth/saas/roles/../users")).isFalse();
        assertThat(filter.isCanonicalSessionPath("/auth/saas/roles//bindings")).isFalse();
        assertThat(filter.isCanonicalSessionPath("/auth/saas/roles;admin=true")).isFalse();

        MockHttpServletRequest request = request("POST", "/auth/saas/roles/%2e%2e/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(chain.getRequest()).isNull();
        verifyNoInteractions(sessionService);
    }

    @Test
    void shouldCheckFreshDatabaseMembershipAndClearTrustedMarker() throws Exception {
        MockHttpServletRequest request = request("POST", "/auth/saas/roles");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        when(sessionService.requireCurrent()).thenReturn(
                new TenantSessionContext("user", "tenant", "member", "app", 2, 7));

        try (MockedStatic<SaSameUtil> same = mockStatic(SaSameUtil.class);
             MockedStatic<StpUtil> login = mockStatic(StpUtil.class)) {
            filter.doFilter(request, response, chain);
            same.verify(SaSameUtil::checkCurrentRequestToken);
            login.verify(StpUtil::checkLogin);
        }

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
        verify(sessionService).requireCurrent();
        assertThat(request.getAttribute(UserContextFilter.VERIFIED_SAAS_AUTH_REQUEST)).isNull();
    }

    @Test
    void shouldAuthenticateTenantSwitchWithoutLegacyUserProfile() throws Exception {
        MockHttpServletRequest request = request("POST", "/auth/switchContext");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        when(sessionService.requireCurrent()).thenReturn(
                new TenantSessionContext("user", "tenant", "member", "app", 2, 7));

        try (MockedStatic<SaSameUtil> same = mockStatic(SaSameUtil.class);
             MockedStatic<StpUtil> login = mockStatic(StpUtil.class)) {
            filter.doFilter(request, response, chain);
            same.verify(SaSameUtil::checkCurrentRequestToken);
            login.verify(StpUtil::checkLogin);
        }

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
        verify(sessionService).requireCurrent();
        assertThat(request.getAttribute(UserContextFilter.VERIFIED_SAAS_AUTH_REQUEST)).isNull();
    }

    @Test
    void shouldRejectRevokedMemberOrAuthorizationVersion() throws Exception {
        MockHttpServletRequest request = request("DELETE",
                "/auth/saas/roles/bindings/1234567890abcdef1234567890abcdef");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        when(sessionService.requireCurrent()).thenThrow(new SecurityException("Version revoked"));

        try (MockedStatic<SaSameUtil> same = mockStatic(SaSameUtil.class);
             MockedStatic<StpUtil> login = mockStatic(StpUtil.class)) {
            filter.doFilter(request, response, chain);
        }

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(chain.getRequest()).isNull();
        assertThat(request.getAttribute(UserContextFilter.VERIFIED_SAAS_AUTH_REQUEST)).isNull();
    }

    @Test
    void shouldRejectMissingServiceTokenBeforeCheckingSubjectSession() throws Exception {
        MockHttpServletRequest request = request("POST", "/auth/saas/roles");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        try (MockedStatic<SaSameUtil> same = mockStatic(SaSameUtil.class);
             MockedStatic<StpUtil> login = mockStatic(StpUtil.class)) {
            same.when(SaSameUtil::checkCurrentRequestToken)
                    .thenThrow(new SecurityException("Service authentication required"));
            filter.doFilter(request, response, chain);
        }

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(chain.getRequest()).isNull();
        verifyNoInteractions(sessionService);
    }

    private MockHttpServletRequest request(String method, String path) {
        return new MockHttpServletRequest(method, path);
    }
}
