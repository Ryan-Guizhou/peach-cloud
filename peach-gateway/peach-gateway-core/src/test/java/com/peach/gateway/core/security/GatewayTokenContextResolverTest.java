package com.peach.gateway.core.security;

import cn.dev33.satoken.reactor.context.SaReactorSyncHolder;
import cn.dev33.satoken.session.SaSession;
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
    void shouldResolveTenantFromTheCurrentSaTokenAndReflectTenantSwitches() {
        String tokenA = UUID.randomUUID().toString();
        String tokenB = UUID.randomUUID().toString();
        SaSession sessionA = StpUtil.getStpLogic().getTokenSessionByToken(tokenA, true);
        SaSession sessionB = StpUtil.getStpLogic().getTokenSessionByToken(tokenB, true);
        setTenantContext(sessionA, "tenant-a", "org-a");
        setTenantContext(sessionB, "tenant-b", "org-b");

        assertThat(resolveCurrentToken(tokenA).tenantId()).isEqualTo("tenant-a");
        assertThat(resolveCurrentToken(tokenB).tenantId()).isEqualTo("tenant-b");

        setTenantContext(sessionB, "tenant-c", "org-c");

        assertThat(resolveCurrentToken(tokenB))
                .isEqualTo(new GatewayTokenContext("tenant-c", "org-c", null));
        assertThat(resolveCurrentToken(tokenA).tenantId()).isEqualTo("tenant-a");
    }

    @Test
    void shouldNotResolveTenantAfterTokenSessionContextIsClearedOrMissing() {
        String token = UUID.randomUUID().toString();
        SaSession session = StpUtil.getStpLogic().getTokenSessionByToken(token, true);
        setTenantContext(session, "tenant-a", "org-a");
        assertThat(resolveCurrentToken(token)).isNotNull();

        session.set(SaTokenConstant.TOKEN_SESSION_TENANT_ID, "");
        session.set(SaTokenConstant.TOKEN_SESSION_ORG_ID, "");

        assertThat(resolveCurrentToken(token)).isNull();
        assertThat(resolveCurrentToken(UUID.randomUUID().toString())).isNull();
    }

    @Test
    void shouldKeepTenantContextIsolatedBetweenTokenSessions() {
        SaSession first = new SaSession("token-session-device-a");
        SaSession second = new SaSession("token-session-device-b");
        first.set(SaTokenConstant.TOKEN_SESSION_TENANT_ID, "tenant-a");
        first.set(SaTokenConstant.TOKEN_SESSION_ORG_ID, "org-a");
        second.set(SaTokenConstant.TOKEN_SESSION_TENANT_ID, "tenant-b");
        second.set(SaTokenConstant.TOKEN_SESSION_ORG_ID, "org-b");
        second.set(SaTokenConstant.TOKEN_SESSION_TENANT_ID, "tenant-c");

        assertThat(resolver.toContext(first).block().tenantId()).isEqualTo("tenant-a");
        assertThat(resolver.toContext(second).block().tenantId()).isEqualTo("tenant-c");
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
