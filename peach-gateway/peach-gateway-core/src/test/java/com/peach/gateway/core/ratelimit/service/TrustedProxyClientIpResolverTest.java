package com.peach.gateway.core.ratelimit.service;

import com.peach.gateway.core.ratelimit.config.GatewayRateLimitProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.InetSocketAddress;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 可信代理客户端 IP 解析测试。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/05
 */
class TrustedProxyClientIpResolverTest {

    @Test
    void shouldIgnoreForwardedHeaderFromUntrustedPeer() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setTrustedProxies(List.of("10.0.0.0/8"));
        TrustedProxyClientIpResolver resolver = new TrustedProxyClientIpResolver(new RateLimitRuleRegistry(properties));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders")
                        .remoteAddress(new InetSocketAddress("203.0.113.10", 8080))
                        .header("X-Forwarded-For", "198.51.100.20")
                        .build());

        assertThat(resolver.resolve(exchange)).isEqualTo("203.0.113.10");
    }

    @Test
    void shouldResolveFirstUntrustedHopFromRightOfTrustedProxyChain() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setTrustedProxies(List.of("10.0.0.0/8"));
        TrustedProxyClientIpResolver resolver = new TrustedProxyClientIpResolver(properties);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders")
                        .remoteAddress(new InetSocketAddress("10.0.0.3", 8080))
                        .header("X-Forwarded-For", "198.51.100.20, 10.0.0.2")
                        .build());

        assertThat(resolver.resolve(exchange)).isEqualTo("198.51.100.20");
    }


    @Test
    void shouldCombineDuplicateForwardedForHeaderFieldsInWireOrder() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setTrustedProxies(List.of("10.0.0.0/8"));
        TrustedProxyClientIpResolver resolver = new TrustedProxyClientIpResolver(properties);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders")
                        .remoteAddress(new InetSocketAddress("10.0.0.3", 8080))
                        .header("X-Forwarded-For", "203.0.113.77")
                        .header("X-Forwarded-For", "198.51.100.20, 10.0.0.2")
                        .build());

        assertThat(resolver.resolve(exchange)).isEqualTo("198.51.100.20");
    }

    @Test
    void shouldFallbackToTcpPeerWhenForwardedChainIsMalformed() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setTrustedProxies(List.of("10.0.0.0/8"));
        TrustedProxyClientIpResolver resolver = new TrustedProxyClientIpResolver(properties);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders")
                        .remoteAddress(new InetSocketAddress("10.0.0.3", 8080))
                        .header("X-Forwarded-For", "198.51.100.20, attacker.example")
                        .build());

        assertThat(resolver.resolve(exchange)).isEqualTo("10.0.0.3");
    }

    @Test
    void shouldKeepPreviousTrustedProxyNetworksWhenInvalidRefreshIsRejected() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setTrustedProxies(List.of("10.0.0.0/8"));
        RateLimitRuleRegistry registry = new RateLimitRuleRegistry(properties);
        TrustedProxyClientIpResolver resolver = new TrustedProxyClientIpResolver(registry);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders")
                        .remoteAddress(new InetSocketAddress("10.0.0.3", 8080))
                        .header("X-Forwarded-For", "198.51.100.20")
                        .build());

        properties.setTrustedProxies(List.of("not-a-cidr"));
        assertThatThrownBy(registry::refresh).isInstanceOf(IllegalArgumentException.class);

        assertThat(resolver.resolve(exchange)).isEqualTo("198.51.100.20");
    }
}}
