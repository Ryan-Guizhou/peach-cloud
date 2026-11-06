package com.peach.gateway.core.security;

import cn.dev33.satoken.session.SaSession;
import com.peach.common.constant.SaTokenConstant;
import org.junit.jupiter.api.Test;

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
}
