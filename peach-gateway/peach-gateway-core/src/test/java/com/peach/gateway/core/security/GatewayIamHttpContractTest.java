package com.peach.gateway.core.security;

import cn.dev33.satoken.same.SaSameUtil;
import com.peach.gateway.core.config.GatewayIamProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 网关与 Auth 的受信 IAM 协议测试：真实 WebClient 编解码，模拟 Auth HTTP 行为。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 14:10
 */
class GatewayIamHttpContractTest {

    private final GatewayTokenContext context =
            new GatewayTokenContext("user", "tenant", "member", "app", 2, 7, null);

    @Test
    void shouldSendBothTrustedCredentialsToAuthAndMatchDatabaseVersion() {
        AtomicReference<ClientRequest> captured = new AtomicReference<>();
        GatewayIamHttpDecisionVerifier verifier = verifier(request -> {
            captured.set(request);
            return Mono.just(response(HttpStatus.OK, validDecision()));
        }, Duration.ofMillis(500));

        Boolean allowed = verifier.requestAuthorization(context, "GET:/api/documents",
                "real-user-token", "service-same-token").block();

        assertThat(allowed).isTrue();
        ClientRequest request = captured.get();
        assertThat(request).isNotNull();
        assertThat(request.method()).isEqualTo(HttpMethod.POST);
        assertThat(request.url().toString()).isEqualTo(
                "http://peach-auth/auth/external/iam/check-api");
        assertThat(request.headers().getFirst("X-Peach-Subject-Token"))
                .isEqualTo("real-user-token");
        assertThat(request.headers().getFirst(SaSameUtil.SAME_TOKEN))
                .isEqualTo("service-same-token");
        assertThat(request.headers().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    }

    @Test
    void shouldDenyMismatchedVersionOrMissingTrustedCredentials() {
        GatewayIamHttpDecisionVerifier verifier = verifier(request -> Mono.just(
                response(HttpStatus.OK, validDecision().replace(
                        "\"authVersion\":7", "\"authVersion\":8"))), Duration.ofMillis(500));

        assertThat(verifier.requestAuthorization(context, "GET:/api/documents",
                "real-user-token", "service-token").block()).isFalse();
        assertThat(verifier.requestAuthorization(context, "GET:/api/documents",
                "", "service-token").block()).isFalse();
        assertThat(verifier.requestAuthorization(context, "GET:/api/documents",
                "real-user-token", "").block()).isFalse();
    }

    @Test
    void shouldFailClosedOnRemoteErrorAndEmptyResponse() {
        GatewayIamHttpDecisionVerifier failed = verifier(request -> Mono.just(
                ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE).build()), Duration.ofMillis(500));
        GatewayIamHttpDecisionVerifier empty = verifier(request -> Mono.just(
                ClientResponse.create(HttpStatus.OK).build()), Duration.ofMillis(500));

        assertThat(failed.requestAuthorization(context, "GET:/api/docs",
                "subject", "same").block()).isFalse();
        assertThat(empty.requestAuthorization(context, "GET:/api/docs",
                "subject", "same").block()).isFalse();
    }

    @Test
    void shouldFailClosedWhenAuthDoesNotRespondWithinDeadline() {
        GatewayIamHttpDecisionVerifier verifier = verifier(request -> Mono.never(),
                Duration.ofMillis(25));

        assertThat(verifier.requestAuthorization(context, "GET:/api/docs",
                "subject", "same").block()).isFalse();
    }

    private GatewayIamHttpDecisionVerifier verifier(
            ExchangeFunction exchange,
            Duration timeout) {
        GatewayIamProperties properties = new GatewayIamProperties();
        properties.setTimeout(timeout);
        return new GatewayIamHttpDecisionVerifier(WebClient.builder()
                .exchangeFunction(exchange), properties);
    }

    private ClientResponse response(HttpStatus status, String json) {
        return ClientResponse.create(status)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(json)
                .build();
    }

    private String validDecision() {
        return "{\"allowed\":true,\"context\":{\"userRefId\":\"user\","
                + "\"tenantRefId\":\"tenant\",\"memberRefId\":\"member\","
                + "\"appRefId\":\"app\",
                + "\"memberVersion\":2,\"authVersion\":7}}";
    }
}
