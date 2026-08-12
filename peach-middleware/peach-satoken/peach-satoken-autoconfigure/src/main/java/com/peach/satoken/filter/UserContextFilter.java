package com.peach.satoken.filter;

import cn.dev33.satoken.stp.StpUtil;
import com.peach.satoken.config.UserContextProperties;
import com.peach.satoken.context.SecurityContextHolder;
import com.peach.satoken.context.UserContext;
import com.peach.satoken.support.UserContextSupport;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Restores the cached current user for an authenticated servlet request.
 */
public class UserContextFilter extends OncePerRequestFilter {

    private final UserContextProperties properties;
    private final UserContextSupport userContextSupport;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public UserContextFilter(UserContextProperties properties, UserContextSupport userContextSupport) {
        this.properties = properties;
        this.userContextSupport = userContextSupport;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        SecurityContextHolder.clear();
        Object loginId = StpUtil.getLoginIdDefaultNull();
        if (loginId == null) {
            if (isPublic(request.getRequestURI())) {
                filterChain.doFilter(request, response);
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Login required");
            }
            return;
        }

        String userId = String.valueOf(loginId).trim();
        if (userId.isEmpty()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Login required");
            return;
        }

        UserContext context = userContextSupport.findByUserId(userId);
        if (context == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Current user cache is unavailable");
            return;
        }

        context.setRequestPath(request.getRequestURI());
        try {
            SecurityContextHolder.set(context);
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clear();
        }
    }

    private boolean isPublic(String path) {
        if (path == null || properties.getPublicPaths() == null) {
            return false;
        }
        for (String pattern : properties.getPublicPaths()) {
            if (pattern != null && pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }


}
