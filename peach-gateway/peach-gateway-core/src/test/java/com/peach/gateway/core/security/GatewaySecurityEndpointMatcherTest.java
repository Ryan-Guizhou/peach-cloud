package com.peach.gateway.core.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 网关匿名白名单不能扩大到 IAM 管理与平台引导入口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 12:05
 */
class GatewaySecurityEndpointMatcherTest {

    private final GatewaySecurityEndpointMatcher matcher = new GatewaySecurityEndpointMatcher();

    @Test
    void shouldNeverMakeSaasManagementOrInitPublic() {
        List<GatewaySecurityEndpointRule> allPublic = List.of(GatewaySecurityEndpointRule.any("/**"));
        assertThat(matcher.matches(allPublic, "PUT",
                "/api/auth/saas/roles/role/permissions")).isFalse();
        assertThat(matcher.matches(allPublic, "POST", "/api/auth/init")).isFalse();
        assertThat(matcher.matches(allPublic, "GET", "/api/files/private")).isFalse();
    }

    @Test
    void shouldKeepExplicitLoginAndOptionsPublic() {
        List<GatewaySecurityEndpointRule> rules = List.of(
                GatewaySecurityEndpointRule.of("OPTIONS", "/**"),
                GatewaySecurityEndpointRule.of("POST", "/api/auth/login"));
        assertThat(matcher.matches(rules, "OPTIONS", "/api/files/private")).isTrue();
        assertThat(matcher.matches(rules, "OPTIONS", "/api/auth/saas/roles")).isTrue();
        assertThat(matcher.matches(rules, "POST", "/api/auth/login")).isTrue();
        assertThat(matcher.matches(rules, "GET", "/api/auth/login")).isFalse();
    }

    @Test
    void shouldRejectConfiguredWildcardsAndAnyMethodForPublicHttpEndpoints() {
        List<GatewaySecurityEndpointRule> rules = List.of(
                GatewaySecurityEndpointRule.of("GET", "/api/files/**"),
                GatewaySecurityEndpointRule.any("/api/files/private"),
                GatewaySecurityEndpointRule.of("POST", "/api/auth/login"));
        assertThat(matcher.matches(rules, "GET", "/api/files/private")).isFalse();
        assertThat(matcher.matches(rules, "DELETE", "/api/files/private")).isFalse();
        assertThat(matcher.matches(rules, "POST", "/api/auth/login")).isTrue();
        assertThat(matcher.matches(rules, "DELETE", "/api/auth/login")).isFalse();
    }

    @Test
    void shouldRejectEncodedOrNonCanonicalPublicRequestPaths() {
        List<GatewaySecurityEndpointRule> rules = List.of(
                GatewaySecurityEndpointRule.of("GET", "/api/auth/login"),
                GatewaySecurityEndpointRule.of("GET", "/api/auth/%6Cogin"));
        assertThat(matcher.matches(rules, "GET", "/api/auth/%6Cogin")).isFalse();
        assertThat(matcher.matches(rules, "GET", "/api/auth/../login")).isFalse();
        assertThat(matcher.matches(rules, "GET", "/api/auth//login")).isFalse();
        assertThat(matcher.matches(rules, "GET", "/api/auth/login")).isTrue();
    }

    @Test
    void shouldAllowOnlyExplicitReadOnlyLoginInitializationMethod() {
        List<GatewaySecurityEndpointRule> rules = List.of(
                GatewaySecurityEndpointRule.of("POST", "/auth/init"),
                GatewaySecurityEndpointRule.of("POST", "/api/auth/init"));
        assertThat(matcher.matches(rules, "POST", "/auth/init")).isTrue();
        assertThat(matcher.matches(rules, "POST", "/api/auth/init")).isTrue();
        assertThat(matcher.matches(rules, "GET", "/api/auth/init")).isFalse();
        assertThat(matcher.matches(rules, "POST", "/api/auth/saas/roles")).isFalse();
        assertThat(matcher.matches(rules, "POST", "/api/auth/external/iam/check-api")).isFalse();
    }

    @Test
    void shouldRejectIamEvenWhenSpecificWildcardConfigured() {
        List<GatewaySecurityEndpointRule> rules =
                List.of(GatewaySecurityEndpointRule.any("/api/auth/**"));
        assertThat(matcher.matches(rules, "POST", "/api/auth/init")).isFalse();
        assertThat(matcher.matches(rules, "PUT",
                "/api/auth/saas/roles/role/permissions")).isFalse();
    }
}
