package com.peach.gateway.core.security;

import cn.dev33.satoken.session.SaSession;
import com.peach.common.constant.SaTokenConstant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 网关 Token Session 租户上下文解析测试。
 */
class GatewayTokenContextResolverTest {

    private final GatewayTokenContextResolver resolver = new GatewayTokenContextResolver();

    @Test
    void shouldReturnEmptyWhenTenantContextIsMissing() {
        assertThat(resolver.toContext(new SaSession()).blockOptional()).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenOrganizationContextIsMissing() {
        SaSession session = new SaSession();
        session.set(SaTokenConstant.TOKEN_SESSION_TENANT_ID, "tenant-a");

        assertThat(resolver.toContext(session).blockOptional()).isEmpty();
    }

    @Test
    void shouldResolveTenantOrganizationAndFiscalFromTokenSession() {
        SaSession session = new SaSession();
        session.set(SaTokenConstant.TOKEN_SESSION_TENANT_ID, "tenant-a");
        session.set(SaTokenConstant.TOKEN_SESSION_ORG_ID, "org-a");
        session.set(SaTokenConstant.TOKEN_SESSION_FISCAL, "2026");

        assertThat(resolver.toContext(session).block())
                .isEqualTo(new GatewayTokenContext("tenant-a", "org-a", "2026"));
    }
}
