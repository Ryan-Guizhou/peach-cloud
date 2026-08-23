package com.peach.observability.web;

import com.peach.observability.config.PeachObservabilityProperties;
import com.peach.observability.core.ObservabilityConstants;
import com.peach.observability.core.RequestIdResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet 请求 ID 过滤器。
 *
 * <p>过滤器解析或生成请求 ID，将其写入请求属性、响应头和 MDC，并在请求完成后恢复原有
 * MDC 值，防止线程复用造成上下文泄漏。过滤器不会记录请求体、查询参数或认证信息。</p>
 */
public final class RequestIdServletFilter extends OncePerRequestFilter {

    private final PeachObservabilityProperties.RequestId properties;
    private final RequestIdResolver resolver;

    /**
     * 创建 Servlet 请求 ID 过滤器。
     *
     * @param properties 请求 ID 配置
     * @param resolver 请求 ID 解析器
     */
    public RequestIdServletFilter(PeachObservabilityProperties.RequestId properties, RequestIdResolver resolver) {
        this.properties = properties;
        this.resolver = resolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = resolver.resolve(request.getHeader(properties.getHeaderName()));
        String previousRequestId = MDC.get(ObservabilityConstants.REQUEST_ID_MDC_KEY);
        request.setAttribute(ObservabilityConstants.REQUEST_ID_ATTRIBUTE, requestId);
        response.setHeader(properties.getHeaderName(), requestId);
        MDC.put(ObservabilityConstants.REQUEST_ID_MDC_KEY, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            restoreMdc(previousRequestId);
        }
    }

    private static void restoreMdc(String previousRequestId) {
        if (previousRequestId == null) {
            MDC.remove(ObservabilityConstants.REQUEST_ID_MDC_KEY);
        } else {
            MDC.put(ObservabilityConstants.REQUEST_ID_MDC_KEY, previousRequestId);
        }
    }
}
