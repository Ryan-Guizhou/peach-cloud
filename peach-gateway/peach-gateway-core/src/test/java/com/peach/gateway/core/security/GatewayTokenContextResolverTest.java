package com.peach.gateway.core.security;

import cn.dev33.satoken.session.SaSession;
import com.peach.common.constant.SaTokenConstant;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 新网关 Token 上下文必须与认证域单快照完全一致，缺失/旧版默认拒绝。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 11:40
 */
class GatewayTokenContextResolverTest {

    private final GatewayTokenContextResolver resolver = new GatewayTokenContextResolver();

    @Test
    void shouldDenyLegacyFiscalAndIndividualTokenSessionKeys() {
        SaSession session = new SaSession("legacy");
        session.set(SaTokenConstant.TOKEN_SESSION_TENANT_ID, "TENANT_BIZ");
        session.set(SaTokenConstant.TOKEN_SESSION_ORG_ID, "ORG_BIZ");
        session.set(SaTokenConstant.TOKEN_SESSION_FISCAL, "2026");
        assertThat(resolver.toContext(session).blockOptional()).isEmpty();
    }

    @Test
    void shouldResolveNewPhysicalIdentityWithoutAnOrganization() {
        SaSession session = new SaSession("saas-valid");
        session.set(SaTokenConstant.TOKEN_SESSION_SAAS_CONTEXT, validContext());
        assertThat(resolver.toContext(session).block())
                .isEqualTo(new GatewayTokenContext(
                        "user", "tenant", "member", "app", 3L, 7L, null));
    }

    @Test
    void shouldNotMixDifferentSessionsOrTenantSwitches() {
        SaSession first = new SaSession("saas-first");
        first.set(SaTokenConstant.TOKEN_SESSION_SAAS_CONTEXT, validContext());
        SaSession second = new SaSession("saas-second");
        Map<String, String> switched = validContext();
        switched.put("tenantRefId", "tenant-2");
        switched.put("memberRefId", "member-2");
        switched.put("authVersion", "14");
        second.set(SaTokenConstant.TOKEN_SESSION_SAAS_CONTEXT, switched);
        assertThat(resolver.toContext(first).block().tenantRefId()).isEqualTo("tenant");
        assertThat(resolver.toContext(second).block().memberRefId()).isEqualTo("member-2");
        assertThat(resolver.toContext(second).block().authVersion()).isEqualTo(14L);
    }

    @Test
    void shouldRejectPartialMalformedOrUnversionedContexts() {
        SaSession session = new SaSession("saas-invalid");
        Map<String, String> value = validContext();
        value.remove("authVersion");
        session.set(SaTokenConstant.TOKEN_SESSION_SAAS_CONTEXT, value);
        assertThat(resolver.toContext(session).blockOptional()).isEmpty();
        value.put("authVersion", "-1");
        assertThat(resolver.toContext(session).blockOptional()).isEmpty();
        value.put("authVersion", "not-a-version");
        assertThat(resolver.toContext(session).blockOptional()).isEmpty();
        value.put("authVersion", "7");
        value.put("year", "2026");
        assertThat(resolver.toContext(session).blockOptional()).isEmpty();
        value.remove("year");
        value.put("memberRefId", " ");
        assertThat(resolver.toContext(session).blockOptional()).isEmpty();
    }

    @Test
    void shouldUseVersionedMemberCacheKeyWithoutReusingFiscalKeys() {
        String key = SaTokenConstant.memberApiResourceCacheKey(
                "tenant", "member", "app", 2026, 3, 7);
        assertThat(key).contains("tenant:member:app:2026:3:7")
                .doesNotContain("fiscal");
        assertThat(SaTokenConstant.memberApiResourceCacheKey(
                "tenant", "member", "app", 2026, 3, 8)).isNotEqualTo(key);
    }

    private static Map<String, String> validContext() {
        Map<String, String> context = new HashMap<>();
        context.put("userRefId", "user");
        context.put("tenantRefId", "tenant");
        context.put("memberRefId", "member");
        context.put("appRefId", "app");
        context.put("memberVersion", "3");
        context.put("authVersion", "7");
        return context;
    }
}
