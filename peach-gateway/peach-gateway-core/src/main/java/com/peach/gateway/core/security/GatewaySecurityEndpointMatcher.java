package com.peach.gateway.core.security;

import java.util.List;
import java.util.Locale;

/**
 * GatewaySecurityEndpointMatcher相关类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/11 14:45
 */
public class GatewaySecurityEndpointMatcher {

    /**
     * 判断当前请求是否命中公开端点规则。
     *
     * @param rules 已配置的端点规则
     * @param method 请求方法
     * @param path 原始请求路径
     * @return 命中公开端点时返回 {@code true}
     */
    public boolean matches(List<GatewaySecurityEndpointRule> rules, String method, String path) {
        if (rules == null || rules.isEmpty() || isBlank(path)) {
            return false;
        }
        String normalizedMethod = normalizeMethod(method);
        String normalizedPath = normalizePath(path);
        // 对原始 URI 做严格检查，禁止 Gateway / Servlet 对编码和路径分隔符产生不同解释。
        if (hasUnsafePath(normalizedPath)) {
            return false;
        }
        // 平台初始化和 SaaS 授权管理绝不能因为配置失误成为匿名接口。
        if (!"OPTIONS".equals(normalizedMethod) && isProtectedIamEndpoint(normalizedPath)) {
            return false;
        }
        for (GatewaySecurityEndpointRule rule : rules) {
            if (rule == null || isBlank(rule.getPath())) {
                continue;
            }
            String rulePath = normalizePath(rule.getPath());
            String ruleMethod = normalizeMethod(rule.getMethod());
            // 仅允许显式 OPTIONS /** 的预检规则；其他匿名规则必须绑定 HTTP 方法和精确路径。
            if ("OPTIONS".equals(normalizedMethod) && "OPTIONS".equals(ruleMethod)
                    && "/**".equals(rulePath)) {
                return true;
            }
            if (GatewaySecurityEndpointRule.ANY_METHOD.equals(ruleMethod)
                    || hasUnsafePath(rulePath) || hasPatternSyntax(rulePath)) {
                continue;
            }
            if (methodMatches(ruleMethod, normalizedMethod) && rulePath.equals(normalizedPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 即使远程配置出现宽泛白名单，IAM 管理入口也不能匿名放行。
     */
    private boolean isProtectedIamEndpoint(String path) {
        return "/auth/init".equals(path) || "/api/auth/init".equals(path)
                || path.startsWith("/auth/external/iam/")
                || "/auth/external/iam".equals(path)
                || path.startsWith("/api/auth/external/iam/")
                || "/api/auth/external/iam".equals(path)
                || path.startsWith("/auth/saas/")
                || "/auth/saas".equals(path)
                || path.startsWith("/api/auth/saas/")
                || "/api/auth/saas".equals(path);
    }

    /**
     * 所有非 OPTIONS 的匿名路径均不能使用 Ant 通配或模板匹配。
     */
    private boolean hasPatternSyntax(String path) {
        return path.contains("*") || path.contains("?")
                || path.contains("{") || path.contains("}");
    }

    /**
     * 防止网关与下游 Servlet 对 URI 规范化方式不同而导致绕过。
     */
    private boolean hasUnsafePath(String path) {
        if (path.indexOf('%') >= 0 || path.indexOf('\\\\') >= 0
                || path.indexOf(';') >= 0 || path.contains("//")) {
            return true;
        }
        for (String part : path.split("/")) {
            if (".".equals(part) || "..".equals(part)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断规则方法是否匹配请求方法。
     *
     * @param ruleMethod 配置的方法，或 {@code ANY}
     * @param requestMethod 标准化后的请求方法
     * @return 方法匹配时返回 {@code true}
     */
    private boolean methodMatches(String ruleMethod, String requestMethod) {
        String normalizedRuleMethod = normalizeMethod(ruleMethod);
        return GatewaySecurityEndpointRule.ANY_METHOD.equals(normalizedRuleMethod)
                || normalizedRuleMethod.equals(requestMethod);
    }

    /**
     * 标准化请求方法。
     *
     * @param method 原始方法值
     * @return 大写方法值；为空时返回 {@code ANY}
     */
    private String normalizeMethod(String method) {
        if (isBlank(method)) {
            return GatewaySecurityEndpointRule.ANY_METHOD;
        }
        return method.trim().toUpperCase(Locale.ENGLISH);
    }

    /**
     * 标准化请求路径，确保 Ant 匹配时带有前导斜杠。
     *
     * @param path 原始路径
     * @return 标准化后的路径
     */
    private String normalizePath(String path) {
        String normalized = path.trim();
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    /**
     * 判断字符串是否为空。
     *
     * @param value 待检查字符串
     * @return 为空时返回 {@code true}
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
