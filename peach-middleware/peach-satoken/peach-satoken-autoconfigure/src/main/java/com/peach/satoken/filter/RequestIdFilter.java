package com.peach.satoken.filter;

import com.peach.satoken.config.RequestIdProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 请求ID过滤器（用于业务服务链路追踪）。
 *
 * <p>该过滤器在每个请求中执行一次，负责处理请求ID（Request ID）：</p>
 * <ul>
 *   <li>从请求头中读取指定名称（通过 {@link RequestIdProperties#getHeaderName()} 配置）的请求ID。</li>
 *   <li>若请求头中存在且符合格式要求（8~64位字母数字下划线连字符），则沿用该ID（通常由上游服务传递）。</li>
 *   <li>若请求头中不存在或格式非法，则自动生成一个新的UUID（去掉连字符）作为请求ID。</li>
 *   <li>将最终的请求ID写入响应头（相同名称），便于下游服务或客户端追踪。</li>
 * </ul>
 *
 * <p><b>格式校验：</b>使用正则 {@code ^[A-Za-z0-9_-]{8,64}$}，确保ID安全且长度合理。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/10/10 15:30
 */
@Slf4j
public class RequestIdFilter extends OncePerRequestFilter {

    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");

    private final RequestIdProperties properties;

    /**
     * 构造器，注入请求ID配置。
     *
     * @param properties 配置项（包含请求头名称）
     */
    public RequestIdFilter(RequestIdProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 1. 解析请求ID（优先使用上游传递的，否则生成新的）
        String requestId = resolveRequestId(request);

        // 2. 将请求ID写入响应头，方便下游链路追踪
        response.setHeader(properties.getHeaderName(), requestId);

        log.debug("RequestID assigned: {} for {} {}", requestId, request.getMethod(), request.getRequestURI());

        // 3. 继续执行过滤器链
        filterChain.doFilter(request, response);
    }

    /**
     * 解析请求ID，优先使用请求头中的值，若无效则生成新的UUID。
     *
     * @param request HTTP请求对象
     * @return 有效的请求ID（保证符合格式要求）
     */
    private String resolveRequestId(HttpServletRequest request) {
        String headerName = properties.getHeaderName();
        String requestId = request.getHeader(headerName);

        if (requestId != null) {
            requestId = requestId.trim();
            // 检查格式是否合法
            if (REQUEST_ID_PATTERN.matcher(requestId).matches()) {
                log.debug("Using request ID from header '{}': {}", headerName, requestId);
                return requestId;
            } else {
                log.warn("Invalid request ID format in header '{}': '{}', will generate new one",
                        headerName, requestId);
            }
        } else {
            log.trace("No request ID found in header '{}', generating new one", headerName);
        }

        // 生成新ID（UUID去掉连字符）
        String newId = UUID.randomUUID().toString().replace("-", "");
        log.debug("Generated new request ID: {}", newId);
        return newId;
    }
}
