package com.peach.openfeign.constant;

import java.util.List;

/**
 * PeachOpenFeign常量。
 * <p>集中定义服务间调用需要传播的 RequestId、重试默认值与异常响应文案。</p>
 * <p>该类只提供跨组件共享的稳定值，不承载动态配置或业务决策逻辑。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 15:30
 */
public final class PeachOpenfeignConstants {

    private PeachOpenfeignConstants() {
        throw new IllegalStateException("Utility class");
    }

    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    public static final String HEADER_AUTHORIZATION = "Authorization";

    public static final String HEADER_RELAY_AUTHORIZATION = "X-Peach-Relay-Authorization";

    public static final int MAX_REQUEST_ID_LENGTH = 128;

    public static final int MAX_AUTHORIZATION_LENGTH = 4096;

    public static final int MAX_SAME_TOKEN_LENGTH = 4096;

    public static final List<String> DEFAULT_RETRY_READ_METHODS = List.of(
            "GET",
            "HEAD"
    );

    public static final List<Integer> DEFAULT_RETRYABLE_STATUSES = List.of(
            429,
            503,
            504
    );

    public static final List<String> DEFAULT_RETRYABLE_EXCEPTIONS = List.of(
            "java.net.ConnectException",
            "java.net.SocketTimeoutException",
            "java.net.UnknownHostException",
            "feign.RetryableException"
    );

    public static final List<String> DEFAULT_PRODUCTION_PROFILES = List.of(
            "prod",
            "production",
            "docker"
    );

    public static final String MESSAGE_UPLOAD_TOO_LARGE = "上传文件过大，请改用直传方案";

    public static final String MESSAGE_SERVICE_UNAVAILABLE_SUFFIX = "服务暂不可用，请稍后重试";

    public static final String MESSAGE_SERVICE_DEGRADED_PREFIX = "服务降级：";

}
