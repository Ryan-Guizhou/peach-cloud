package com.peach.gateway.core.filter;

import cn.dev33.satoken.same.SaSameUtil;
import com.peach.gateway.core.constant.GatewayConstant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 公网入口必须清理客户端提供的内部服务凭证，不能带入任何下游服务。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 13:40
 */
class GatewayRequestGlobalFilterTest {

    private final GatewayRequestGlobalFilter filter = new GatewayRequestGlobalFilter();

    @Test
    void shouldStripSpoofedInternalTokensAndReplaceUntrustedRequestId() {
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/login")
                        .header(SaSameUtil.SAME_TOKEN, "untrusted-service-credential")
                        .header("X-Peach-Subject-Token", "forged-subject")
                        .header(GatewayConstant.REQUEST_ID_HEADER, "untrusted-request-id")
                        .build());

        filter.filter(exchange, chain).block();

        ArgumentCaptor<ServerWebExchange> captured = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captured.capture());
        var headers = captured.getValue().getRequest().getHeaders();
        assertThat(headers.getFirst(SaSameUtil.SAME_TOKEN)).isNull();
        assertThat(headers.getFirst("X-Peach-Subject-Token")).isNull();
        assertThat(headers.getFirst(GatewayConstant.REQUEST_ID_HEADER))
                .hasSize(32).isNotEqualTo("untrusted-request-id");
        assertThat(captured.getValue().getResponse().getHeaders()
                .getFirst(GatewayConstant.REQUEST_ID_HEADER))
                .isEqualTo(headers.getFirst(GatewayConstant.REQUEST_ID_HEADER));
    }

    @Test
    void shouldKeepOrdinaryEndUserAuthorizationHeader() {
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/files/read")
                        .header("Authorization", "user-session-token")
                        .header(SaSameUtil.SAME_TOKEN, "spoofed")
                        .build());

        filter.filter(exchange, chain).block();

        ArgumentCaptor<ServerWebExchange> captured = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captured.capture());
        var headers = captured.getValue().getRequest().getHeaders();
        assertThat(headers.getFirst("Authorization")).isEqualTo("user-session-token");
        assertThat(headers.getFirst(SaSameUtil.SAME_TOKEN)).isNull();
    }
}
