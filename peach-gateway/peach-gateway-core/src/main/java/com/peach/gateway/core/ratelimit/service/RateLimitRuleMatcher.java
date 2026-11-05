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

    public Optional<MatchedRateLimitRule> match(RateLimitRule rule, ServerWebExchange exchange,
                                                String tenantId, String clientIp) {
        String method = exchange.getRequest().getMethod().name().toUpperCase(Locale.ROOT);
        String path = exchange.getRequest().getURI().getRawPath();
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String routeId = route == null ? null : route.getId();

        if (!rule.methods().isEmpty()
                && !rule.methods().contains(method)
                && !rule.methods().contains("ANY")
                && !rule.methods().contains("*")) {
            return Optional.empty();
        }
        if (!rule.routeIds().isEmpty() && (routeId == null || !rule.routeIds().contains(routeId))) {
            return Optional.empty();
        }
        if (rule.scope().isTenantScope() && (tenantId == null || tenantId.isBlank())) {
            return Optional.empty();
        }
        if (rule.scope().isIpScope() && (clientIp == null || clientIp.isBlank())) {
            return Optional.empty();
        }

        if (!rule.scope().isUriScope()) {
            return Optional.of(new MatchedRateLimitRule(rule, ""));
        }
        return rule.paths().stream()
                .filter(pattern -> pathMatcher.match(pattern, path))
                .findFirst()
                .map(pattern -> new MatchedRateLimitRule(rule, pattern));
    }
}
