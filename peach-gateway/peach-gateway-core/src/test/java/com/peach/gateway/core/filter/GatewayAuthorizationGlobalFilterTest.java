package com.peach.gateway.core.filter;

import com.peach.gateway.core.config.GatewaySaTokenProperties;
import com.peach.gateway.core.security.GatewayTokenContextResolver;
import com.peach.gateway.core.security.GatewaySecurityEndpointRule;
import com.peach.gateway.core.security.GatewayIamDecisionVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import reactor.core.publisher.Mono;

/**
 * 认证配置关闭时网关必须拒绝受保护路由，不得按历史开关直接放行。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 12:05
 */
class GatewayAuthorizationGlobalFilterTest {

    @Test
    void disabledAuthenticationMustFailClosedForProtectedEndpoints() {
        GatewaySaTokenProperties props = new GatewaySaTokenProperties();
        props.setEnabled(false);
        GatewayAuthorizationGlobalFilter filter = filter(props);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/saas/roles").build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        verify(chain, never()).filter(any());
    }

    @Test
    void explicitlyPublicLoginCanStillBeCalledWhenAuthSubsystemUnavailable() {
        GatewaySaTokenProperties props = new GatewaySaTokenProperties();
        props.setEnabled(false);
        GatewayAuthorizationGlobalFilter filter = filter(props);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/auth/login").build());

        filter.filter(exchange, chain).block();

        verify(chain).filter(any());
        assertThat(exchange.getResponse().getStatusCode())
                .isNotEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldRejectInternalIamPathEvenWhenPublicRuleIsAdded() {
        GatewaySaTokenProperties props = new GatewaySaTokenProperties();
        var rules = new ArrayList<>(props.getPublicEndpoints());
        rules.add(GatewaySecurityEndpointRule.any(
                "/api/auth/external/iam/**"));
        props.setPublicEndpoints(rules);
        GatewayAuthorizationGlobalFilter filter = filter(props);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/auth/external/iam/check-api").build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(chain, never()).filter(any());
    }

    @SuppressWarnings("unchecked")
    private static GatewayAuthorizationGlobalFilter filter(GatewaySaTokenProperties properties) {
        ObjectProvider<GatewayIamDecisionVerifier> verifiers =
                (ObjectProvider<GatewayIamDecisionVerifier>) mock(ObjectProvider.class);
        when(verifiers.getIfAvailable()).thenReturn(null);
        return new GatewayAuthorizationGlobalFilter(
                properties, verifiers, mock(GatewayTokenContextResolver.class));
    }
}
