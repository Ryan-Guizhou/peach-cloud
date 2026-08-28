package com.peach.gateway.core.security;

import org.springframework.util.AntPathMatcher;

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

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

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
        for (GatewaySecurityEndpointRule rule : rules) {
            if (rule == null || isBlank(rule.getPath())) {
                continue;
            }
            if (methodMatches(rule.getMethod(), normalizedMethod)
                    && pathMatcher.match(normalizePath(rule.getPath()), normalizedPath)) {
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
