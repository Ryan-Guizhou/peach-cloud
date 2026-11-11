package com.peach.openfeign.quickstart;

import com.peach.openfeign.config.PeachOpenfeignProperties;
import com.peach.openfeign.support.PeachOpenfeignRetryPolicy;
import feign.Request;
import feign.RetryableException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证网络异常不得绕过 OpenFeign HTTP 方法重试白名单。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/11
 */
class OpenFeignRetryPolicyTest {

    private final PeachOpenfeignRetryPolicy policy =
            new PeachOpenfeignRetryPolicy(new PeachOpenfeignProperties());

    @Test
    void shouldAllowConfiguredReadMethod() {
        assertThat(policy.canRetryException(failure(Request.HttpMethod.GET))).isTrue();
    }

    @Test
    void shouldRejectWriteMethodEvenForRetryableException() {
        assertThat(policy.canRetryException(failure(Request.HttpMethod.POST))).isFalse();
    }

    @Test
    void shouldRespectDisabledRetryOption() {
        PeachOpenfeignProperties properties = new PeachOpenfeignProperties();
        properties.getRetry().setEnabled(false);
        PeachOpenfeignRetryPolicy disabled = new PeachOpenfeignRetryPolicy(properties);
        assertThat(disabled.canRetryException(failure(Request.HttpMethod.GET))).isFalse();
    }

    private static RetryableException failure(Request.HttpMethod method) {
        Request request = Request.create(method, "http://localhost/test",
                Map.of(), null, StandardCharsets.UTF_8, null);
        return new RetryableException(-1, "network failure", method,
                new IllegalStateException("timeout"), (Long) null, request);
    }
}
