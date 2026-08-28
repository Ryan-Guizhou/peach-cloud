package com.peach.observability.core;

import com.peach.observability.config.PeachObservabilityProperties;

/**
 * 请求Id解析器。
 * <p>仅接受 ASCII 字母、数字、下划线和连字符，防止控制字符和日志注入。公网入口可以关闭
 * 上游信任，此时无论客户端是否传值都生成新的可信请求 ID。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
public final class RequestIdResolver {

    private final PeachObservabilityProperties.RequestId properties;
    private final RequestIdGenerator generator;

    /**
     * 创建请求 ID 解析器。
     *
     * @param properties 请求 ID 配置
     * @param generator 请求 ID 生成器
     */
    public RequestIdResolver(PeachObservabilityProperties.RequestId properties, RequestIdGenerator generator) {
        validateProperties(properties);
        this.properties = properties;
        this.generator = generator;
    }

    /**
     * 解析上游请求 ID；未信任或格式不合法时生成新值。
     *
     * @param incomingRequestId 上游请求头值，可以为空
     * @return 可安全传播和记录的请求 ID
     */
    public String resolve(String incomingRequestId) {
        if (properties.isTrustIncoming() && isValid(incomingRequestId)) {
            return incomingRequestId.trim();
        }
        String generated = generator.generate();
        if (!isValid(generated)) {
            throw new IllegalStateException("RequestIdGenerator returned an invalid request ID");
        }
        return generated;
    }

    /**
     * 判断请求 ID 是否符合安全格式。
     *
     * @param requestId 待检查值
     * @return 合法时返回 {@code true}
     */
    public boolean isValid(String requestId) {
        if (requestId == null) {
            return false;
        }
        String value = requestId.trim();
        if (value.length() < properties.getMinLength() || value.length() > properties.getMaxLength()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            boolean allowed = character >= 'a' && character <= 'z'
                    || character >= 'A' && character <= 'Z'
                    || character >= '0' && character <= '9'
                    || character == '_'
                    || character == '-';
            if (!allowed) {
                return false;
            }
        }
        return true;
    }

    private static void validateProperties(PeachObservabilityProperties.RequestId properties) {
        if (properties == null) {
            throw new IllegalArgumentException("Request ID properties must not be null");
        }
        if (properties.getHeaderName() == null || properties.getHeaderName().isBlank()) {
            throw new IllegalArgumentException("Request ID header name must not be blank");
        }
        if (properties.getMinLength() < 1 || properties.getMaxLength() < properties.getMinLength()) {
            throw new IllegalArgumentException("Request ID length range is invalid");
        }
    }
}
