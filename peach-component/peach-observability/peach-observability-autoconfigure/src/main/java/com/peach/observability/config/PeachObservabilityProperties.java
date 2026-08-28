package com.peach.observability.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * PeachObservability配置属性。
 * <p>该配置只管理项目自定义的请求关联能力。指标、链路导出、采样率和管理端点继续使用
 * Spring Boot 标准的 {@code management.*} 配置，避免形成两套含义相同的配置体系。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
@ConfigurationProperties(prefix = "peach.observability")
public class PeachObservabilityProperties {

    /** 是否启用 Peach Cloud 可观测性自动配置。 */
    private boolean enabled = true;

    /** 请求关联标识配置。 */
    private final RequestId requestId = new RequestId();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public RequestId getRequestId() {
        return requestId;
    }

    /**
     * 请求Id。
     * <p>请求 ID 用于用户侧问题定位，不替代 OpenTelemetry 的 traceId 和 spanId。</p>
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */
    public static class RequestId {

        /** 是否启用 Servlet 请求 ID 过滤器。 */
        private boolean enabled = true;

        /** 请求头和响应头使用的名称。 */
        private String headerName = "X-Request-Id";

        /** 是否信任格式合法的上游请求 ID。业务服务通常开启，公网网关应关闭。 */
        private boolean trustIncoming = true;

        /** 请求 ID 最小长度。 */
        private int minLength = 8;

        /** 请求 ID 最大长度。 */
        private int maxLength = 64;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getHeaderName() {
            return headerName;
        }

        public void setHeaderName(String headerName) {
            this.headerName = headerName;
        }

        public boolean isTrustIncoming() {
            return trustIncoming;
        }

        public void setTrustIncoming(boolean trustIncoming) {
            this.trustIncoming = trustIncoming;
        }

        public int getMinLength() {
            return minLength;
        }

        public void setMinLength(int minLength) {
            this.minLength = minLength;
        }

        public int getMaxLength() {
            return maxLength;
        }

        public void setMaxLength(int maxLength) {
            this.maxLength = maxLength;
        }
    }
}
