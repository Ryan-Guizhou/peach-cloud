package com.peach.satoken.filter;

import cn.dev33.satoken.stp.StpUtil;
import com.peach.satoken.config.UserContextProperties;
import com.peach.satoken.context.SecurityContextHolder;
import com.peach.satoken.context.UserContext;
import com.peach.satoken.security.SatokenEndpointMatcher;
import com.peach.satoken.support.UserContextSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 当前用户上下文恢复过滤器。
 *
 * <p>该过滤器只作用于引入 {@code peach-satoken-starter} 的 Servlet 业务服务。公开端点直接放行；
 * 非公开端点必须先存在 Sa-Token 登录态，再按 loginId 从 Redis 用户上下文 Hash 中恢复
 * {@link UserContext}，并绑定到 {@link SecurityContextHolder} 供当前请求链路使用。</p>
 *
 * <p>过滤器不会写入 Redis，也不会刷新用户上下文缓存。登录、切换租户/机构或用户资料变更后的缓存写入，
 * 必须由认证服务按 {@link UserContextSupport} 约定的字段完成。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/10/10 15:30
 */
@Slf4j
public class UserContextFilter extends OncePerRequestFilter {

    private final UserContextProperties properties;
    private final UserContextSupport userContextSupport;
    private final SatokenEndpointMatcher endpointMatcher = new SatokenEndpointMatcher();

    /**
     * 创建当前用户上下文恢复过滤器。
     *
     * @param properties         用户上下文配置，包含公开端点白名单
     * @param userContextSupport Redis 用户上下文读取组件
     */
    public UserContextFilter(UserContextProperties properties, UserContextSupport userContextSupport) {
        this.properties = properties;
        this.userContextSupport = userContextSupport;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String method = request.getMethod();
        String path = request.getRequestURI();

        SecurityContextHolder.clear();

        if (isPublic(method, path)) {
            log.debug("User context loading skipped for public endpoint, method={}, path={}", method, path);
            filterChain.doFilter(request, response);
            return;
        }

        Object loginId = StpUtil.getLoginIdDefaultNull();
        if (loginId == null) {
            log.warn("User context loading rejected because loginId is missing, method={}, path={}", method, path);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Login required");
            return;
        }

        String userId = String.valueOf(loginId).trim();
        if (userId.isEmpty()) {
            log.warn("User context loading rejected because loginId is empty, method={}, path={}", method, path);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Login required");
            return;
        }

        UserContext context = userContextSupport.getUserContextByUserId(userId);
        if (context == null) {
            log.warn("User context cache unavailable, userId={}, method={}, path={}", userId, method, path);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Current user cache is unavailable");
            return;
        }

        context.setRequestPath(path);
        try {
            SecurityContextHolder.set(context);
            log.debug("User context loaded, userId={}, method={}, path={}", userId, method, path);
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clear();
            log.trace("SecurityContextHolder cleared, userId={}", userId);
        }
    }

    /**
     * 判断当前请求是否命中公开端点白名单。
     *
     * @param method HTTP 请求方法
     * @param path   Servlet 请求路径
     * @return 命中公开端点白名单时返回 {@code true}
     */
    private boolean isPublic(String method, String path) {
        return endpointMatcher.matches(properties.getPublicEndpoints(), method, path);
    }
}
