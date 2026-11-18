package com.peach.auth.rest.filter;

import cn.dev33.satoken.same.SaSameUtil;
import cn.dev33.satoken.stp.StpUtil;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.satoken.filter.UserContextFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * SaaS 角色管理的 Auth 专用 Token 成员验证过滤器。
 *
 * <p>该过滤器顺序必须早于通用 UserContextFilter（order 0）。首先核验
 * Same-Token 服务身份、Sa-Token 最终用户登录态及数据库中的有效租户、成员、
 * 应用、成员版本和授权版本。只有全部通过才设置 Servlet 请求内部属性，
 * 让通用过滤器跳过已经废弃的用户级 USER_PROFILE 缓存。
 * 具体角色及行/字段权限仍由领域服务另行校验。该属性不能由 HTTP 头生成。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 13:30
 */
@Slf4j
@Component
@Order(-100)
@RequiredArgsConstructor
public class SaasRoleTokenContextFilter extends OncePerRequestFilter {

    private static final String ROLE_ROOT = "/auth/saas/roles";

    private final TenantSessionService tenantSessionService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !(ROLE_ROOT.equals(path) || path.startsWith(ROLE_ROOT + "/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!isCanonicalRolePath(request.getRequestURI())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid SaaS role path");
            return;
        }
        try {
            SaSameUtil.checkCurrentRequestToken();
            StpUtil.checkLogin();
            if (tenantSessionService.requireCurrent() == null) {
                throw new SecurityException("Verified SaaS member context is required");
            }
        } catch (RuntimeException exception) {
            log.warn("SaaS role request rejected, method={}, reason={}",
                    request.getMethod(), exception.getClass().getSimpleName());
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "SaaS authorization required");
            return;
        }

        request.setAttribute(UserContextFilter.VERIFIED_SAAS_ROLE_REQUEST, Boolean.TRUE);
        try {
            filterChain.doFilter(request, response);
        } finally {
            request.removeAttribute(UserContextFilter.VERIFIED_SAAS_ROLE_REQUEST);
        }
    }
    /**
     * 在认证服务直接访问场景下也拒绝编码路径和 Servlet 路径规范化歧义。
     */
    boolean isCanonicalRolePath(String path) {
        if (path == null || !(ROLE_ROOT.equals(path) || path.startsWith(ROLE_ROOT + "/"))
                || path.indexOf('%') >= 0 || path.indexOf(';') >= 0
                || path.indexOf('\\') >= 0 || path.contains("//")
                || path.endsWith("/") || path.contains("/./") || path.contains("/../")
                || path.endsWith("/.") || path.endsWith("/..")) {
            return false;
        }
        return true;
    }

}
