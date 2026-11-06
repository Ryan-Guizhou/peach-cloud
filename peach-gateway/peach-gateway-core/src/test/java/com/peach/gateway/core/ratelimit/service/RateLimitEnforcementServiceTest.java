package com.peach.gateway.core.ratelimit.service;

import com.peach.gateway.core.ratelimit.config.GatewayRateLimitProperties;
import com.peach.gateway.core.ratelimit.model.RateLimitAlgorithm;
import com.peach.gateway.core.ratelimit.model.RateLimitCheckResult;
import com.peach.gateway.core.ratelimit.model.RateLimitDecision;
import com.peach.gateway.core.ratelimit.model.RateLimitScope;
import com.peach.gateway.core.ratelimit.script.RateLimitScriptRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 限流规则编排测试。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/05
 */
class RateLimitEnforcementServiceTest {

    @Test
    void shouldStopAtFirstRejectedRuleInPriorityOrder() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setEnabled(true);
        properties.setRules(List.of(
                rule("global", RateLimitScope.GLOBAL, RateLimitAlgorithm.TOKEN_BUCKET, 10),
                rule("ip", RateLimitScope.IP, RateLimitAlgorithm.FIXED_WINDOW, 20)));

        RedisRateLimitExecutor executor = mock(RedisRateLimitExecutor.class);
        TrustedProxyClientIpResolver ipResolver = mock(TrustedProxyClientIpResolver.class);
        RateLimitTenantContextResolver tenantResolver = mock(RateLimitTenantContextResolver.class);
        RateLimitMetrics metrics = mock(RateLimitMetrics.class);
        when(ipResolver.resolve(any())).thenReturn("192.0.2.10");
        when(executor.execute(any(), anyString()))
                .thenReturn(Mono.just(new RateLimitDecision(true, 5, 1000, 0, 10)))
                .thenReturn(Mono.just(new RateLimitDecision(false, 0, 1000, 1500, 10)));

        RateLimitEnforcementService service = service(properties, executor, ipResolver, tenantResolver, metrics);
        RateLimitCheckResult result = service.check(exchange()).block(Duration.ofSeconds(3));

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(RateLimitCheckResult.Status.REJECTED);
        assertThat(result.rule().id()).isEqualTo("ip");
        verify(executor, org.mockito.Mockito.times(2)).execute(any(), anyString());
    }

    @Test
    void shouldRejectTenantRuleWhenTrustedTokenSessionContextIsMissing() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setEnabled(true);
        properties.setRequireTenantContext(true);
        properties.setRules(List.of(
                rule("tenant-api", RateLimitScope.TENANT_URI, RateLimitAlgorithm.TOKEN_BUCKET, 10)));

        RedisRateLimitExecutor executor = mock(RedisRateLimitExecutor.class);
        TrustedProxyClientIpResolver ipResolver = mock(TrustedProxyClientIpResolver.class);
        RateLimitTenantContextResolver tenantResolver = mock(RateLimitTenantContextResolver.class);
        RateLimitMetrics metrics = mock(RateLimitMetrics.class);
        when(ipResolver.resolve(any())).thenReturn("192.0.2.10");
        when(tenantResolver.resolve()).thenReturn(Mono.empty());

        RateLimitEnforcementService service = service(properties, executor, ipResolver, tenantResolver, metrics);
        RateLimitCheckResult result = service.check(exchange()).block(Duration.ofSeconds(3));

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(RateLimitCheckResult.Status.TENANT_CONTEXT_MISSING);
        verify(executor, never()).execute(any(), anyString());
    }

    @Test
    void shouldBypassAllRulesForAllowlistedIp() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setEnabled(true);
        properties.setAllowlistIps(List.of("192.0.2.10"));
        properties.setRules(List.of(
                rule("global", RateLimitScope.GLOBAL, RateLimitAlgorithm.TOKEN_BUCKET, 10)));

        RedisRateLimitExecutor executor = mock(RedisRateLimitExecutor.class);
        TrustedProxyClientIpResolver ipResolver = mock(TrustedProxyClientIpResolver.class);
        RateLimitTenantContextResolver tenantResolver = mock(RateLimitTenantContextResolver.class);
        RateLimitMetrics metrics = mock(RateLimitMetrics.class);
        when(ipResolver.resolve(any())).thenReturn("192.0.2.10");

        RateLimitEnforcementService service = service(properties, executor, ipResolver, tenantResolver, metrics);
        RateLimitCheckResult result = service.check(exchange()).block(Duration.ofSeconds(3));

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(RateLimitCheckResult.Status.ALLOWED);
        verify(executor, never()).execute(any(), anyString());
    }

    @Test
    void shouldApplyGlobalRulesWhenTenantContextIsOptionalAndMissing() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setEnabled(true);
        properties.setRequireTenantContext(false);
        properties.setRules(List.of(
                rule("global", RateLimitScope.GLOBAL, RateLimitAlgorithm.TOKEN_BUCKET, 10),
                rule("tenant", RateLimitScope.TENANT, RateLimitAlgorithm.TOKEN_BUCKET, 20)));

        RedisRateLimitExecutor executor = mock(RedisRateLimitExecutor.class);
        TrustedProxyClientIpResolver ipResolver = mock(TrustedProxyClientIpResolver.class);
        RateLimitTenantContextResolver tenantResolver = mock(RateLimitTenantContextResolver.class);
        RateLimitMetrics metrics = mock(RateLimitMetrics.class);
        when(ipResolver.resolve(any())).thenReturn("192.0.2.10");
        when(tenantResolver.resolve()).thenReturn(Mono.empty());
        when(executor.execute(any(), anyString()))
                .thenReturn(Mono.just(new RateLimitDecision(true, 8, 1000, 0, 10)));

        RateLimitEnforcementService service = service(properties, executor, ipResolver, tenantResolver, metrics);
        RateLimitCheckResult result = service.check(exchange()).block(Duration.ofSeconds(3));

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(RateLimitCheckResult.Status.ALLOWED);
        assertThat(result.rule().id()).isEqualTo("global");
        verify(executor, org.mockito.Mockito.times(1)).execute(any(), anyString());
    }


    @Test
    void shouldEnforceTenantDenylistWithoutTenantScopedRules() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setEnabled(true);
        properties.setDenylistTenants(List.of("tenant-blocked"));
        properties.setRules(List.of(
                rule("global", RateLimitScope.GLOBAL, RateLimitAlgorithm.TOKEN_BUCKET, 10)));

        RedisRateLimitExecutor executor = mock(RedisRateLimitExecutor.class);
        TrustedProxyClientIpResolver ipResolver = mock(TrustedProxyClientIpResolver.class);
        RateLimitTenantContextResolver tenantResolver = mock(RateLimitTenantContextResolver.class);
        RateLimitMetrics metrics = mock(RateLimitMetrics.class);
        when(ipResolver.resolve(any())).thenReturn("192.0.2.10");
        when(tenantResolver.resolve()).thenReturn(Mono.just("tenant-blocked"));

        RateLimitEnforcementService service = service(properties, executor, ipResolver, tenantResolver, metrics);
        RateLimitCheckResult result = service.check(exchange()).block(Duration.ofSeconds(3));

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(RateLimitCheckResult.Status.BLOCKED);
        verify(executor, never()).execute(any(), anyString());
    }

    @Test
    void shouldEnforceTenantDenylistWhenNoRateLimitRuleMatches() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setEnabled(true);
        properties.setDenylistTenants(List.of("tenant-blocked"));
        properties.setRules(List.of());

        RedisRateLimitExecutor executor = mock(RedisRateLimitExecutor.class);
        TrustedProxyClientIpResolver ipResolver = mock(TrustedProxyClientIpResolver.class);
        RateLimitTenantContextResolver tenantResolver = mock(RateLimitTenantContextResolver.class);
        RateLimitMetrics metrics = mock(RateLimitMetrics.class);
        when(ipResolver.resolve(any())).thenReturn("192.0.2.10");
        when(tenantResolver.resolve()).thenReturn(Mono.just("tenant-blocked"));

        RateLimitEnforcementService service = service(properties, executor, ipResolver, tenantResolver, metrics);
        RateLimitCheckResult result = service.check(exchange()).block(Duration.ofSeconds(3));

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(RateLimitCheckResult.Status.BLOCKED);
        verify(executor, never()).execute(any(), anyString());
    }

    @Test
    void shouldEnforceTenantDenylistBeforeIpAllowlist() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setEnabled(true);
        properties.setAllowlistIps(List.of("192.0.2.10"));
        properties.setDenylistTenants(List.of("tenant-blocked"));
        properties.setRules(List.of());

        RedisRateLimitExecutor executor = mock(RedisRateLimitExecutor.class);
        TrustedProxyClientIpResolver ipResolver = mock(TrustedProxyClientIpResolver.class);
        RateLimitTenantContextResolver tenantResolver = mock(RateLimitTenantContextResolver.class);
        RateLimitMetrics metrics = mock(RateLimitMetrics.class);
        when(ipResolver.resolve(any())).thenReturn("192.0.2.10");
        when(tenantResolver.resolve()).thenReturn(Mono.just("tenant-blocked"));

        RateLimitEnforcementService service = service(properties, executor, ipResolver, tenantResolver, metrics);
        RateLimitCheckResult result = service.check(exchange()).block(Duration.ofSeconds(3));

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(RateLimitCheckResult.Status.BLOCKED);
        verify(executor, never()).execute(any(), anyString());
    }

    @Test
    void shouldBypassGlobalRulesForTenantAllowlistedWithoutTenantRules() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setEnabled(true);
        properties.setAllowlistTenants(List.of("tenant-allowed"));
        properties.setRules(List.of(
                rule("global", RateLimitScope.GLOBAL, RateLimitAlgorithm.TOKEN_BUCKET, 10)));

        RedisRateLimitExecutor executor = mock(RedisRateLimitExecutor.class);
        TrustedProxyClientIpResolver ipResolver = mock(TrustedProxyClientIpResolver.class);
        RateLimitTenantContextResolver tenantResolver = mock(RateLimitTenantContextResolver.class);
        RateLimitMetrics metrics = mock(RateLimitMetrics.class);
        when(ipResolver.resolve(any())).thenReturn("192.0.2.10");
        when(tenantResolver.resolve()).thenReturn(Mono.just("tenant-allowed"));

        RateLimitEnforcementService service = service(properties, executor, ipResolver, tenantResolver, metrics);
        RateLimitCheckResult result = service.check(exchange()).block(Duration.ofSeconds(3));

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(RateLimitCheckResult.Status.ALLOWED);
        verify(executor, never()).execute(any(), anyString());
    }

    @Test
    void shouldKeepEnforcementInactiveUntilAllScriptsAreReady() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setEnabled(true);
        properties.setRules(List.of(
                rule("global", RateLimitScope.GLOBAL, RateLimitAlgorithm.TOKEN_BUCKET, 10)));

        RedisRateLimitExecutor executor = mock(RedisRateLimitExecutor.class);
        TrustedProxyClientIpResolver ipResolver = mock(TrustedProxyClientIpResolver.class);
        RateLimitTenantContextResolver tenantResolver = mock(RateLimitTenantContextResolver.class);
        RateLimitMetrics metrics = mock(RateLimitMetrics.class);
        RateLimitScriptRepository scriptRepository = mock(RateLimitScriptRepository.class);
        when(scriptRepository.isReady()).thenReturn(false);

        RateLimitEnforcementService service = new RateLimitEnforcementService(properties,
                new RateLimitRuleRegistry(properties), new RateLimitRuleMatcher(), new RateLimitKeyFactory(),
                executor, ipResolver, tenantResolver, metrics, scriptRepository);
        RateLimitCheckResult result = service.check(exchange()).block(Duration.ofSeconds(3));

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(RateLimitCheckResult.Status.ALLOWED);
        verify(ipResolver).resolve(any());
        verify(executor, never()).execute(any(), anyString());
        verify(metrics).recordPolicy("scripts-not-ready");
    }

    private RateLimitEnforcementService service(GatewayRateLimitProperties properties,
                                                RedisRateLimitExecutor executor,
                                                TrustedProxyClientIpResolver ipResolver,
                                                RateLimitTenantContextResolver tenantResolver,
                                                RateLimitMetrics metrics) {
        RateLimitScriptRepository scriptRepository = mock(RateLimitScriptRepository.class);
        when(scriptRepository.isReady()).thenReturn(true);
        return new RateLimitEnforcementService(properties, new RateLimitRuleRegistry(properties),
                new RateLimitRuleMatcher(), new RateLimitKeyFactory(), executor,
                ipResolver, tenantResolver, metrics, scriptRepository);
    }

    private GatewayRateLimitProperties.RuleProperties rule(String id, RateLimitScope scope,
                                                            RateLimitAlgorithm algorithm, int priority) {
        GatewayRateLimitProperties.RuleProperties rule = new GatewayRateLimitProperties.RuleProperties();
        rule.setId(id);
        rule.setScope(scope);
        rule.setAlgorithm(algorithm);
        rule.setPriority(priority);
        rule.setBurstCapacity(10);
        rule.setLimit(10);
        rule.setWindow(Duration.ofSeconds(1));
        return rule;
    }

    private MockServerWebExchange exchange() {
        return MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders/1")
                        .remoteAddress(new InetSocketAddress("192.0.2.10", 8080))
                        .build());
    }
}
