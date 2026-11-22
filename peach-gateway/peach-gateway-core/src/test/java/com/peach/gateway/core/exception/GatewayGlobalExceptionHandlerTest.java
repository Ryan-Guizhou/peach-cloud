package com.peach.gateway.core.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peach.common.response.Response;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证上游故障识别及统一响应契约。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 22:43
 */
class GatewayGlobalExceptionHandlerTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final GatewayGlobalExceptionHandler handler = new GatewayGlobalExceptionHandler(mapper);

    @Test
    void upstreamFailuresShouldUseUnavailableMessageWithExistingResponseContract() throws Exception {
        for (Throwable failure : new Throwable[]{new TimeoutException(),
                new IllegalStateException(new ConnectException()),
                io.netty.handler.timeout.ReadTimeoutException.INSTANCE}) {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/test"));
            handler.handle(exchange, failure).block();
            var body = mapper.readTree(exchange.getResponse().getBodyAsString().block());
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(body.get("code").asText()).isEqualTo(Response.fail("ignored").getCode());
            assertThat(body.get("msg").asText()).isEqualTo("服务暂不可用，请稍后重试");
        }
    }

    @Test
    void unrelatedFailureShouldKeepGenericMessage() throws Exception {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/test"));
        handler.handle(exchange, new IllegalArgumentException()).block();
        var body = mapper.readTree(exchange.getResponse().getBodyAsString().block());
        assertThat(body.get("msg").asText()).isEqualTo("网关处理异常，请稍后重试");
    }
}
