package com.peach.message.core.filter;

import cn.dev33.satoken.stp.StpUtil;
import com.peach.message.core.context.ContextDTO;
import com.peach.message.core.context.WebSocketContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/2/4 18:14
 */
@Slf4j
@Component
@WebFilter(filterName = "WebSocketFilter", urlPatterns = "/webSocket/**")
public class WebSocketFilter implements Filter, Ordered {

    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        try {
            HttpServletRequest request = (HttpServletRequest) servletRequest;
            // 多种判断条件组合
            boolean isWebSocketRequest = isWebSocketUpgradeRequest(request)
                    || isWebSocketProtocolRequest(request)
                    || isWebSocketPath(request);

            if (!isWebSocketRequest) {
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            }
            String token = request.getHeader("Sec-WebSocket-Protocol");
            String userId = "";
            boolean isLogin = true;
            if (StringUtils.isBlank(token)) {
                isLogin = false;
            } else {
                Object loginId = StpUtil.getLoginIdByToken(token);
                if (loginId == null) {
                    isLogin = false;
                } else {
                    userId = String.valueOf(loginId);
                    if (StringUtils.isBlank(userId)) {
                        isLogin = false;
                    }
                }
            }
            if (isLogin) {
                response.setHeader("Sec-WebSocket-Protocol", token);
                ContextDTO context = ContextDTO.builder()
                        .userId(userId)
                        .userToken(token)
                        .userIp(servletRequest.getRemoteAddr())
                        .userHost(servletRequest.getRemoteHost())
                        .build();
                WebSocketContext.setContext(context);
            }
            filterChain.doFilter(servletRequest, servletResponse);
        } catch (Exception e) {
            response.setStatus(500);
            log.error("WebSocketFilter error", e);
        }

    }

    @Override
    public void destroy() {
        log.info("WebSocketFilter has been destory");
        Filter.super.destroy();
    }

    @Override
    public int getOrder() {
        return 1;
    }


    private boolean isWebSocketUpgradeRequest(HttpServletRequest request) {
        String connection = request.getHeader("Connection");
        String upgrade = request.getHeader("Upgrade");
        return "Upgrade".equalsIgnoreCase(connection)
                && "webSocket".equalsIgnoreCase(upgrade);
    }

    private boolean isWebSocketProtocolRequest(HttpServletRequest request) {
        String protocol = request.getHeader("Sec-WebSocket-Protocol");
        return StringUtils.isNotBlank(protocol);
    }

    private boolean isWebSocketPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.contains("/ws/") || path.contains("/webSocket/");
    }
}
