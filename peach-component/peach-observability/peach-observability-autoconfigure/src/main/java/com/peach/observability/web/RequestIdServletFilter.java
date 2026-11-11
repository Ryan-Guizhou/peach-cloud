package com.peach.observability.web;

import com.peach.observability.config.PeachObservabilityProperties;
import com.peach.observability.core.ObservabilityConstants;
import com.peach.observability.core.RequestIdResolver;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 请求IdServlet过滤器。
 * <p>过滤器解析或生成请求 ID，将其写入请求属性、响应头和 MDC，并在请求完成后恢复原有
 * MDC 值，防止线程复用造成上下文泄漏。过滤器不会记录请求体、查询参数或认证信息。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
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

    /**
     * 异步重新分派发生在线程切换后，必须重新设置并恢复本线程的请求 MDC。
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Object existing = request.getAttribute(ObservabilityConstants.REQUEST_ID_ATTRIBUTE);
        String requestId = request.getDispatcherType() == DispatcherType.ASYNC
                && existing instanceof String value && !value.isBlank()
                ? value : resolver.resolve(request.getHeader(properties.getHeaderName()));
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
