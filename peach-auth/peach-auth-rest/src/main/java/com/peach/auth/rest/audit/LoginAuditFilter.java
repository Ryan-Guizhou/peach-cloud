package com.peach.auth.rest.audit;

import com.peach.auth.service.support.LoginAuditContext;
import com.peach.auth.service.support.LoginAuditProperties;
import com.peach.auth.service.support.LoginLogWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.MDC;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 认证请求审计边界：不读取请求体，仅采集白名单元数据；审计故障不改变认证返回。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 20:00
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 20)
@RequiredArgsConstructor
public class LoginAuditFilter extends OncePerRequestFilter {
    private final LoginLogWriter writer;
    private final LoginAuditProperties properties;
    private static final Set<String> PATHS = Set.of("/auth/login", "/auth/logout", "/auth/checkCaptcha");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !PATHS.contains(path(request));
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String path = path(request);
        String event = path.endsWith("logout") ? "LOGOUT" : path.endsWith("checkCaptcha") ? "CAPTCHA" : "LOGIN";
        String previousTrace = MDC.get("traceId");
        String trace = previousTrace;
        if (trace == null || !trace.matches("[0-9a-fA-F]{16,32}")) trace = UUID.randomUUID().toString().replace("-", "");
        LoginAuditContext.begin(event, sourceIp(request), browser(request.getHeader("User-Agent")), trace);
        MDC.put("traceId", trace);
        if ("CAPTCHA".equals(event)) LoginAuditContext.reason("CAPTCHA_FAILED");
        try { chain.doFilter(request, response); }
        catch (IOException | ServletException | RuntimeException ex) { LoginAuditContext.reason("INTERNAL_ERROR"); throw ex; }
        finally {
            try {
                var row = LoginAuditContext.finish();
                if (row != null && !("CAPTCHA".equals(event) && "SUCCESS".equals(row.getResult()))) writer.save(row);
            } catch (RuntimeException ex) {
                log.error("Login audit write failed, errorType={}", ex.getClass().getSimpleName());
            } finally {
                LoginAuditContext.clear();
                if (previousTrace == null) MDC.remove("traceId"); else MDC.put("traceId", previousTrace);
            }
        }
    }
    private static String path(HttpServletRequest request) { return request.getRequestURI().substring(request.getContextPath().length()); }

    /** 只从显式信任的直连代理解析转发链，并从右向左剥离可信跳点；不解析主机名。 */
    public String sourceIp(HttpServletRequest request) {
        String current = request.getRemoteAddr();
        if (!properties.getTrustedProxies().contains(current)) return safeIp(current);
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.length() > 1024) return safeIp(current);
        String[] hops = forwarded.split(",");
        for (int i = hops.length - 1; i >= 0 && properties.getTrustedProxies().contains(current); i--) {
            String candidate = hops[i].trim();
            if ("unknown".equals(safeIp(candidate))) return safeIp(request.getRemoteAddr());
            current = candidate;
        }
        return safeIp(current);
    }
    private static String safeIp(String value) {
        if (value == null || !value.matches("[0-9a-fA-F:.]{1,45}")) return "unknown";
        if (!value.contains(":") && !value.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")) return "unknown";
        try { return java.net.InetAddress.getByName(value).getHostAddress(); }
        catch (java.net.UnknownHostException ex) { return "unknown"; }
    }
    private static String browser(String value) {
        String ua = value == null ? "" : value.substring(0, Math.min(value.length(), 1024)).toLowerCase(Locale.ROOT);
        String family = ua.contains("edg/") ? "Edge" : ua.contains("firefox/") ? "Firefox" : ua.contains("chrome/") ? "Chrome" : ua.contains("safari/") ? "Safari" : "Other";
        String os = ua.contains("android") ? "Android" : ua.contains("iphone") || ua.contains("ipad") ? "iOS" : ua.contains("windows") ? "Windows" : ua.contains("macintosh") ? "macOS" : ua.contains("linux") ? "Linux" : "Other";
        return family + " / " + os;
    }
}
