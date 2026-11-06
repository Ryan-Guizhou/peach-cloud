package com.peach.gateway.core.filter;

import com.peach.gateway.core.ratelimit.config.GatewayRateLimitProperties;
import com.peach.gateway.core.ratelimit.model.RateLimitAlgorithm;
import com.peach.gateway.core.ratelimit.model.RateLimitCheckResult;
import com.peach.gateway.core.ratelimit.model.RateLimitDecision;
import com.peach.gateway.core.ratelimit.model.RateLimitRule;
import com.peach.gateway.core.ratelimit.model.RateLimitScope;
import com.peach.gateway.core.ratelimit.service.RateLimitEnforcementService;
import com.peach.gateway.core.ratelimit.service.RateLimitMetrics;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 网关限流过滤器响应与故障策略测试。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/05
 */
class GatewayRateLimitGlobalFilterTest {

    @Test
    void shouldReturn429WithStandardRateLimitHeaders() {
        RateLimitEnforcementService service = mock(RateLimitEnforcementService.class);
        RateLimitMetrics metrics = mock(RateLimitMetrics.class);
        RateLimitRule rule = rule();
        RateLimitDecision decision = new RateLimitDecision(false, 0, 1500, 1200, 10);
        when(service.isEnabled()).thenReturn(true);
        when(service.check(any())).thenReturn(Mono.just(RateLimitCheckResult.rejected(rule, decision)));

        GatewayRateLimitGlobalFilter filter = new GatewayRateLimitGlobalFilter(service, metrics);
        MockServerWebExchange exchange = exchange();
        AtomicBoolean forwarded = new AtomicBoolean();
        GatewayFilterChain chain = current -> {
            forwarded.set(true);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block(Duration.ofSeconds(3));

        assertThat(forwarded).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exchange.getResponse().getHeaders().getFirst("RateLimit-Limit")).isEqualTo("10");
        assertThat(exchange.getResponse().getHeaders().getFirst("RateLimit-Remaining")).isEqualTo("0");
        assertThat(exchange.getResponse().getHeaders().getFirst("RateLimit-Reset")).isEqualTo("2");
        assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("2");
    }

    @Test
    void shouldFailClosedWith503WhenRedisExecutionFails() {
        RateLimitEnforcementService service = mock(RateLimitEnforcementService.class);
        RateLimitMetrics metrics = mock(RateLimitMetrics.class);
        when(service.isEnabled()).thenReturn(true);
        when(service.failureMode(any())).thenReturn(GatewayRateLimitProperties.FailureMode.FAIL_CLOSED);
        when(service.check(any())).thenReturn(Mono.error(new IllegalStateException("redis unavailable")));

        GatewayRateLimitGlobalFilter filter = new GatewayRateLimitGlobalFilter(service, metrics);
        MockServerWebExchange exchange = exchange();
        AtomicBoolean forwarded = new AtomicBoolean();

        filter.filter(exchange, current -> {
            forwarded.set(true);
            return Mono.empty();
        }).block(Duration.ofSeconds(3));

        assertThat(forwarded).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void shouldFailOpenWhenConfiguredAndRedisExecutionFails() {
        RateLimitEnforcementService service = mock(RateLimitEnforcementService.class);
        RateLimitMetrics metrics = mock(RateLimitMetrics.class);
        when(service.isEnabled()).thenReturn(true);
        when(service.failureMode(any())).thenReturn(GatewayRateLimitProperties.FailureMode.FAIL_OPEN);
        when(service.check(any())).thenReturn(Mono.error(new IllegalStateException("redis unavailable")));

        GatewayRateLimitGlobalFilter filter = new GatewayRateLimitGlobalFilter(service, metrics);
        MockServerWebExchange exchange = exchange();
        AtomicBoolean forwarded = new AtomicBoolean();

        filter.filter(exchange, current -> {
            forwarded.set(true);
            return Mono.empty();
        }).block(Duration.ofSeconds(3));

        assertThat(forwarded).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    private RateLimitRule rule() {
        return new RateLimitRule("global", true, RateLimitScope.GLOBAL, RateLimitAlgorithm.TOKEN_BUCKET,
                1, Set.of(), List.of(), Set.of(), 10, 0, 10D, 10, 10D, 1);
    }

    private MockServerWebExchange exchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders/1").build());
    }
}
