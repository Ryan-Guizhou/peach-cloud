package com.peach.gateway.core.security;

import cn.dev33.satoken.reactor.context.SaReactorSyncHolder;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.dev33.satoken.stp.StpUtil;
import com.peach.common.constant.SaTokenConstant;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 网关 Token Session 租户上下文解析测试。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/06
 */
class GatewayTokenContextResolverTest {

    private final GatewayTokenContextResolver resolver = new GatewayTokenContextResolver();

    @Test
    void shouldReturnEmptyWhenTenantContextIsMissing() {
        assertThat(resolver.toContext(new SaSession()).blockOptional()).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenOrganizationContextIsMissing() {
        SaSession session = new SaSession("token-session-missing-org");
        session.set(SaTokenConstant.TOKEN_SESSION_TENANT_ID, "tenant-a");

        assertThat(resolver.toContext(session).blockOptional()).isEmpty();
    }

    @Test
    void shouldResolveTenantOrganizationAndFiscalFromTokenSession() {
        SaSession session = new SaSession("token-session-complete");
        session.set(SaTokenConstant.TOKEN_SESSION_TENANT_ID, "tenant-a");
        session.set(SaTokenConstant.TOKEN_SESSION_ORG_ID, "org-a");
        session.set(SaTokenConstant.TOKEN_SESSION_FISCAL, "2026");

        assertThat(resolver.toContext(session).block())
                .isEqualTo(new GatewayTokenContext("tenant-a", "org-a", "2026"));
    }

    @Test
void shouldResolveTenantFromIndependentDeviceTokensAndReflectTenantSwitches() {
        String loginId = "token-context-" + UUID.randomUUID();
        String tokenA = loginWithTenantContext(loginId, "device-a", "tenant-a", "org-a");
        String tokenB = loginWithTenantContext(loginId, "device-b", "tenant-b", "org-b");

        try {
            assertThat(resolveCurrentToken(tokenA))
                    .isEqualTo(new GatewayTokenContext("tenant-a", "org-a", null));
            assertThat(resolveCurrentToken(tokenB))
                    .isEqualTo(new GatewayTokenContext("tenant-b", "org-b", null));

            updateCurrentTokenContext(tokenB, "tenant-c", "org-c");

            assertThat(resolveCurrentToken(tokenB))
                    .isEqualTo(new GatewayTokenContext("tenant-c", "org-c", null));
            assertThat(resolveCurrentToken(tokenA))
                    .isEqualTo(new GatewayTokenContext("tenant-a", "org-a", null));
        } finally {
            logoutLoginId(loginId);
        }
    }
    @Test
void shouldNotResolveTenantWhenCurrentTokenSessionContextIsCleared() {
        String loginId = "token-context-clear-" + UUID.randomUUID();
        String token = loginWithTenantContext(loginId, "device-a", "tenant-a", "org-a");

        try {
            assertThat(resolveCurrentToken(token)).isNotNull();

            updateCurrentTokenContext(token, "", "");

            assertThat(resolveCurrentToken(token)).isNull();
        } finally {
            logoutLoginId(loginId);
        }
    }
    @Test
    void shouldKeepTenantContextIsolatedBetweenTokenSessions() {
        SaSession first = new SaSession("token-session-device-a");
        SaSession second = new SaSession("token-session-device-b");
        first.set(SaTokenConstant.TOKEN_SESSION_TENANT_ID, "tenant-a");
        first.set(SaTokenConstant.TOKEN_SESSION_ORG_ID, "org-a");
        second.set(SaTokenConstant.TOKEN_SESSION_TENANT_ID, "tenant-b");
        second.set(SaTokenConstant.TOKEN_SESSION_ORG_ID, "org-b");

        assertThat(resolver.toContext(first).block().tenantId()).isEqualTo("tenant-a");
        assertThat(resolver.toContext(second).block().tenantId()).isEqualTo("tenant-b");
    }
    private String loginWithTenantContext(String loginId,
                                          String device,
                                          String tenantId,
                                          String orgId) {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/auth/login").build());
        SaReactorSyncHolder.setContext(exchange);
        try {
            StpUtil.login(loginId, new SaLoginParameter()
                    .setIsConcurrent(true)
                    .setIsShare(false)
                    .setDeviceId(device));
            String token = StpUtil.getTokenValue();
            setTenantContext(StpUtil.getTokenSession(), tenantId, orgId);
            return token;
        } finally {
            SaReactorSyncHolder.clearContext();
        }
    }

    private void updateCurrentTokenContext(String token, String tenantId, String orgId) {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/context")
                        .header(StpUtil.getTokenName(), token)
                        .build());
        SaReactorSyncHolder.setContext(exchange);
        try {
            setTenantContext(StpUtil.getTokenSession(), tenantId, orgId);
        } finally {
            SaReactorSyncHolder.clearContext();
        }
    }

    private void logoutLoginId(String loginId) {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/auth/logout").build());
        SaReactorSyncHolder.setContext(exchange);
        try {
            StpUtil.logout(loginId);
        } finally {
            SaReactorSyncHolder.clearContext();
        }
    }

    private void setTenantContext(SaSession session, String tenantId, String orgId) {
        session.set(SaTokenConstant.TOKEN_SESSION_TENANT_ID, tenantId);
        session.set(SaTokenConstant.TOKEN_SESSION_ORG_ID, orgId);
    }

    private GatewayTokenContext resolveCurrentToken(String token) {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders")
                        .header(StpUtil.getTokenName(), token)
                        .build());
        SaReactorSyncHolder.setContext(exchange);
        try {
            return resolver.resolve(exchange).block(Duration.ofSeconds(3));
        } finally {
            SaReactorSyncHolder.clearContext();
        }
    }


}
