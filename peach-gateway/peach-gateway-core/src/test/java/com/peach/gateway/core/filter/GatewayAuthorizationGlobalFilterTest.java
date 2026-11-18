package com.peach.gateway.core.filter;

import com.peach.gateway.core.config.GatewaySaTokenProperties;
import com.peach.gateway.core.security.GatewayTokenContextResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

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

    @SuppressWarnings("unchecked")
    private static GatewayAuthorizationGlobalFilter filter(GatewaySaTokenProperties properties) {
        ObjectProvider<ReactiveStringRedisTemplate> redis =
                (ObjectProvider<ReactiveStringRedisTemplate>) mock(ObjectProvider.class);
        when(redis.getIfAvailable()).thenReturn(null);
        return new GatewayAuthorizationGlobalFilter(
                properties, redis, mock(GatewayTokenContextResolver.class));
    }
}
