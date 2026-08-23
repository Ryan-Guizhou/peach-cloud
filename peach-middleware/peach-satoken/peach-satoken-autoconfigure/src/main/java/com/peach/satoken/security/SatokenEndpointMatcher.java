package com.peach.satoken.security;

import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Locale;

/**
 * 业务服务侧 Sa-Token 公开端点匹配器。
 *
 * <p>匹配器会对 HTTP 方法做大写标准化，并对路径补齐前导斜杠，保证
 * Nacos 配置、默认配置和 Servlet 请求路径使用一致的匹配规则。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/10/10 15:30
 */
public class SatokenEndpointMatcher {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 判断请求是否命中公开端点规则。
     *
     * @param rules 公开端点规则列表
     * @param method 当前请求 HTTP 方法
     * @param path 当前请求路径
     * @return 命中公开端点时返回 {@code true}
     */
    public boolean matches(List<SatokenEndpointRule> rules, String method, String path) {
        if (rules == null || rules.isEmpty() || isBlank(path)) {
            return false;
        }
        String normalizedMethod = normalizeMethod(method);
        String normalizedPath = normalizePath(path);
        for (SatokenEndpointRule rule : rules) {
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
     * @param ruleMethod 规则中的 HTTP 方法
     * @param requestMethod 标准化后的请求方法
     * @return 方法匹配时返回 {@code true}
     */
    private boolean methodMatches(String ruleMethod, String requestMethod) {
        String normalizedRuleMethod = normalizeMethod(ruleMethod);
        return SatokenEndpointRule.ANY_METHOD.equals(normalizedRuleMethod)
                || normalizedRuleMethod.equals(requestMethod);
    }

    /**
     * 标准化 HTTP 方法。
     *
     * @param method 原始 HTTP 方法
     * @return 大写方法名；为空时返回 {@link SatokenEndpointRule#ANY_METHOD}
     */
    private String normalizeMethod(String method) {
        if (isBlank(method)) {
            return SatokenEndpointRule.ANY_METHOD;
        }
        return method.trim().toUpperCase(Locale.ENGLISH);
    }

    /**
     * 标准化请求路径。
     *
     * @param path 原始路径
     * @return 带前导斜杠的路径
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
