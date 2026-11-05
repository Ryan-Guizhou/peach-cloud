package com.peach.gateway.core.ratelimit.service;

import com.peach.gateway.core.ratelimit.model.MatchedRateLimitRule;
import com.peach.gateway.core.ratelimit.model.RateLimitRule;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;

import java.util.Locale;
import java.util.Optional;

/**
 * 匹配请求适用的限流规则。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/05
 */
@Component
public class RateLimitRuleMatcher {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 判断请求是否满足规则的路径、方法和路由选择器，不依赖身份信息。
     */
    public boolean matchesRequest(RateLimitRule rule, ServerWebExchange exchange) {
        String method = exchange.getRequest().getMethod().name().toUpperCase(Locale.ROOT);
        String path = normalizePath(exchange.getRequest().getURI().getRawPath());
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String routeId = route == null ? null : route.getId();

        if (!rule.methods().isEmpty()
                && !rule.methods().contains(method)
                && !rule.methods().contains("ANY")
                && !rule.methods().contains("*")) {
            return false;
        }
        if (!rule.routeIds().isEmpty() && (routeId == null || !rule.routeIds().contains(routeId))) {
            return false;
        }
        return rule.paths().isEmpty() || rule.paths().stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    public Optional<MatchedRateLimitRule> match(RateLimitRule rule, ServerWebExchange exchange,
                                                String tenantId, String clientIp) {
        if (!matchesRequest(rule, exchange)) {
            return Optional.empty();
        }
        if (rule.scope().isTenantScope() && isBlank(tenantId)) {
            return Optional.empty();
        }
        if (rule.scope().isIpScope() && isBlank(clientIp)) {
            return Optional.empty();
        }

        String path = normalizePath(exchange.getRequest().getURI().getRawPath());
        String matchedPattern = rule.paths().isEmpty()
                ? (rule.scope().isUriScope() ? path : "")
                : rule.paths().stream()
                .filter(pattern -> pathMatcher.match(pattern, path))
                .findFirst()
                .orElse("");
        return Optional.of(new MatchedRateLimitRule(rule, matchedPattern));
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        String normalized = path.trim();
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
