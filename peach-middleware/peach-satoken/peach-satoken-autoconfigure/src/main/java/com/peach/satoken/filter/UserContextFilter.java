package com.peach.satoken.filter;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.same.SaSameUtil;
import com.peach.satoken.config.UserContextProperties;
import com.peach.satoken.context.SecurityContextHolder;
import com.peach.satoken.context.UserContext;
import com.peach.satoken.security.SatokenEndpointMatcher;
import com.peach.satoken.security.SatokenEndpointRule;
import com.peach.satoken.support.UserContextSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 用户上下文过滤器。
 * <p>该过滤器只作用于引入 {@code peach-satoken-starter} 的 Servlet 业务服务。公开端点直接放行；
 * 非公开端点必须先存在 Sa-Token 登录态，再按 loginId 从 Redis 用户上下文 Hash 中恢复
 * {@link UserContext}，并绑定到 {@link SecurityContextHolder} 供当前请求链路使用。</p>
 * <p>过滤器不会写入 Redis，也不会刷新用户上下文缓存。登录、切换租户/机构或用户资料变更后的缓存写入，
 * 必须由认证服务按 {@link UserContextSupport} 约定的字段完成。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/10/10 15:30
 */
@Slf4j
public class UserContextFilter extends OncePerRequestFilter {

    /** 仅由认证服务前置校验器成功核验数据库成员版本后设置的服务器端请求属性。 */
    public static final String VERIFIED_SAAS_ROLE_REQUEST =
            UserContextFilter.class.getName() + ".verifiedSaasRoleRequest";

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

        // SaaS 角色管理请求已由 Auth 前置过滤器完成 Same-Token 和数据库成员版本核验。
        // 仅允许该请求的服务端内部属性绕开旧 USER_PROFILE Hash；不能从 HTTP 头传入。
        if (Boolean.TRUE.equals(request.getAttribute(VERIFIED_SAAS_ROLE_REQUEST))
                && ("/auth/saas/roles".equals(path) || path.startsWith("/auth/saas/roles/"))) {
            try {
                filterChain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clear();
            }
            return;
        }

        if (isServiceTokenOnly(method, path)) {
            // 仅内部受信服务可跳过旧的 USER_PROFILE Redis Hash。
            // 在实际访问 Controller 前强制校验 Same-Token，不能用公开白名单替代。
            try {
                SaSameUtil.checkCurrentRequestToken();
            } catch (RuntimeException exception) {
                log.warn("Trusted service-only context rejected, method={}, path={}", method, path);
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Service authentication required");
                return;
            }
            try {
                filterChain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clear();
            }
            return;
        }

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
     * 服务专用接口只按显式 HTTP 方法和完整规范路径匹配。
     *
     * <p>严禁在该配置使用 ANY、Ant 通配或路径变量，否则一次错误 Nacos
     * 配置可能让整个业务服务绕过普通用户上下文。虽然仍需 Same-Token，
     * 但不能将服务身份错误提升为任何用户的登录身份。</p>
     *
     * @param method 请求真实 HTTP 方法
     * @param path Servlet 请求 URI
     * @return 仅精确匹配时启用受信服务通道
     */
    boolean isServiceTokenOnly(String method, String path) {
        if (method == null || method.isBlank() || unsafeServicePath(path)) {
            return false;
        }
        List<SatokenEndpointRule> rules =
                properties.getServiceTokenOnlyEndpoints();
        if (rules == null || rules.isEmpty()) {
            return false;
        }
        for (SatokenEndpointRule rule : rules) {
            if (rule == null || rule.getMethod() == null || rule.getPath() == null
                    || rule.getMethod().isBlank() || rule.getPath().isBlank()
                    || rule.getMethod().equalsIgnoreCase("ANY")
                    || !rule.getMethod().trim().equalsIgnoreCase(method)
                    || unsafeServicePath(rule.getPath())
                    || containsPatternSyntax(rule.getPath())) {
                continue;
            }
            if (rule.getPath().equals(path)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsPatternSyntax(String path) {
        return path.indexOf('*') >= 0 || path.indexOf('?') >= 0
                || path.indexOf('{') >= 0 || path.indexOf('}') >= 0;
    }

    private boolean unsafeServicePath(String path) {
        if (path == null || !path.startsWith("/") || path.indexOf('%') >= 0
                || path.indexOf(';') >= 0 || path.indexOf('\\') >= 0
                || path.contains("//") || path.endsWith("/.") || path.endsWith("/..")) {
            return true;
        }
        for (String part : path.split("/")) {
            if (".".equals(part) || "..".equals(part)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPublic(String method, String path) {
        return endpointMatcher.matches(properties.getPublicEndpoints(), method, path);
    }
}
