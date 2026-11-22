package com.peach.gateway.core.filter;

import com.peach.gateway.core.config.GatewaySaTokenProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 认证基础设施故障响应回归测试。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/20
 */
class GatewayAuthorizationGlobalFilterTest {

    @Test
    void shouldReturn503WithoutInvalidatingLoginWhenRedisIsUnavailable() {
        assertFailure(new RedisConnectionFailureException("Redis unavailable"),
                HttpStatus.SERVICE_UNAVAILABLE, "Authentication service unavailable");
    }

    @Test
    void shouldReturn500ForUnexpectedAuthenticationFailure() {
        assertFailure(new IllegalStateException("Internal failure"),
                HttpStatus.INTERNAL_SERVER_ERROR, "Authentication service error");
    }

    private void assertFailure(Throwable failure, HttpStatus status, String message) {
        GatewayAuthorizationGlobalFilter filter = new GatewayAuthorizationGlobalFilter(
                new GatewaySaTokenProperties(), mock(ObjectProvider.class), null);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/profile").build());
        Mono<Void> response = ReflectionTestUtils.invokeMethod(filter, "handleAuthenticationError",
                exchange, "GET", "/api/auth/profile", failure);
        assertThat(response).isNotNull();
        response.block(Duration.ofSeconds(3));
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(status);
        assertThat(exchange.getResponse().getBodyAsString().block()).contains(message);
    }
}
