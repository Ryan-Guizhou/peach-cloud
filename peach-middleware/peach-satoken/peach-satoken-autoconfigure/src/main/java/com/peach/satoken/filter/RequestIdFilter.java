package com.peach.satoken.filter;

import com.peach.satoken.config.RequestIdProperties;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Servlet request id filter for business services.
 *
 * <p>The filter keeps a valid upstream request id or creates a new one, then
 * exposes it through the response header.</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/8/4
 */
public class RequestIdFilter extends OncePerRequestFilter {

    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");

    private final RequestIdProperties properties;

    public RequestIdFilter(RequestIdProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        response.setHeader(properties.getHeaderName(), requestId);
        filterChain.doFilter(request, response);
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(properties.getHeaderName());
        if (requestId != null) {
            requestId = requestId.trim();
        }
        if (requestId == null || !REQUEST_ID_PATTERN.matcher(requestId).matches()) {
            return UUID.randomUUID().toString().replace("-", "");
        }
        return requestId;
    }
}
