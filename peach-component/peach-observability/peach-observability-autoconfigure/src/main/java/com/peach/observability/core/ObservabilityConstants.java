package com.peach.observability.core;

/**
 * Observability常量。
 * <p>这些常量只描述稳定的技术协议，不包含任何业务域语义。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
public final class ObservabilityConstants {

    /** 默认请求 ID 请求头。 */
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    /** 请求 ID 在 MDC 中的键。 */
    public static final String REQUEST_ID_MDC_KEY = "requestId";

    /** 请求 ID 在 Servlet 请求属性中的键。 */
    public static final String REQUEST_ID_ATTRIBUTE = ObservabilityConstants.class.getName() + ".requestId";

    private ObservabilityConstants() {
    }
}
