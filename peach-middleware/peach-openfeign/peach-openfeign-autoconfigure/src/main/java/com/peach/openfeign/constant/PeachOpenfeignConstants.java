package com.peach.openfeign.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * OpenFeign 模块公共常量。
 *
 * <p>集中定义服务间调用需要传播的 RequestId、重试默认值与异常响应文案。</p>
 *
 * <p>该类只提供跨组件共享的稳定值，不承载动态配置或业务决策逻辑。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 15:30
 */
public interface PeachOpenfeignConstants {

    String HEADER_REQUEST_ID = "X-Request-Id";

    int MAX_REQUEST_ID_LENGTH = 128;

    int MAX_SAME_TOKEN_LENGTH = 4096;

    List<String> DEFAULT_RETRY_READ_METHODS = Collections.unmodifiableList(Arrays.asList(
            "GET",
            "HEAD"
    ));

    List<Integer> DEFAULT_RETRYABLE_STATUSES = Collections.unmodifiableList(Arrays.asList(
            429,
            503,
            504
    ));

    List<String> DEFAULT_RETRYABLE_EXCEPTIONS = Collections.unmodifiableList(Arrays.asList(
            "java.net.ConnectException",
            "java.net.SocketTimeoutException",
            "java.net.UnknownHostException",
            "feign.RetryableException"
    ));

   List<String> DEFAULT_PRODUCTION_PROFILES = Collections.unmodifiableList(Arrays.asList(
            "prod",
            "production",
            "docker"
    ));

    String MESSAGE_UPLOAD_TOO_LARGE = "上传文件过大，请改用直传方案";

    String MESSAGE_SERVICE_UNAVAILABLE_SUFFIX = "服务暂不可用，请稍后重试";

    String MESSAGE_SERVICE_DEGRADED_PREFIX = "服务降级：";

}
