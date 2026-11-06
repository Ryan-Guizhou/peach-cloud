package com.peach.gateway.core.filter;

import cn.dev33.satoken.reactor.context.SaReactorSyncHolder;
import com.peach.gateway.core.constant.GatewayConstant;
import com.peach.gateway.core.ratelimit.config.GatewayRateLimitProperties;
import com.peach.gateway.core.ratelimit.constant.GatewayRateLimitConstants;
import com.peach.gateway.core.ratelimit.model.RateLimitCheckResult;
import com.peach.gateway.core.ratelimit.model.RateLimitDecision;
import com.peach.gateway.core.ratelimit.model.RateLimitRule;
import com.peach.gateway.core.ratelimit.service.RateLimitEnforcementService;
import com.peach.gateway.core.ratelimit.service.RateLimitMetrics;
import com.peach.gateway.core.support.GatewayFilterSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关多算法限流全局过滤器。
 *
 * <p>顺序位于 Sa-Token 鉴权之后、Same-Token 注入之前。租户上下文由当前 Token Session 提供，
 * 不接受客户端自定义租户请求头。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/05
 */
@Slf4j
@Indexed
@Component
@RequiredArgsConstructor
public class GatewayRateLimitGlobalFilter implements GlobalFilter, Ordered {

    private static final int FILTER_ORDER = -175;

    private final GatewayRateLimitProperties properties;
    private final RateLimitEnforcementService enforcementService;
    private final RateLimitMetrics metrics;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }

        SaReactorSyncHolder.setContext(exchange);
        return enforcementService.check(exchange)
                .onErrorResume(error -> {
                    log.warn("Gateway rate limit check failed, requestId={}, reason={}",
                            requestId(exchange), error.getClass().getSimpleName());
                    return Mono.just(RateLimitCheckResult.unavailable(error.getClass().getSimpleName()));
                })
                .flatMap(result -> handleResult(exchange, chain, result))
                .doFinally(signal -> SaReactorSyncHolder.clearContext());
    }

    private Mono<Void> handleResult(ServerWebExchange exchange,
                                    GatewayFilterChain chain,
                                    RateLimitCheckResult result) {
        return switch (result.status()) {
            case ALLOWED -> {
                if (result.rule() != null && result.decision() != null) {
                    applyRateLimitHeaders(exchange, result.rule(), result.decision());
                }
                yield chain.filter(exchange);
            }
            case REJECTED -> rejectByQuota(exchange, result.rule(), result.decision());
            case BLOCKED -> {
                log.warn("Gateway rate limit policy rejected request, requestId={}, reason={}",
                        requestId(exchange), result.reason());
                yield GatewayFilterSupport.writeError(exchange, HttpStatus.FORBIDDEN,
                        "Request blocked by gateway rate limit policy");
            }
            case TENANT_CONTEXT_MISSING -> {
                log.warn("Gateway tenant rate limit rejected request, requestId={}, reason={}",
                        requestId(exchange), result.reason());
                yield GatewayFilterSupport.writeError(exchange, HttpStatus.UNAUTHORIZED,
                        "Trusted tenant context is required");
            }
            case UNAVAILABLE -> handleUnavailable(exchange, chain,
                    new IllegalStateException(result.reason()));
        };
    }

    private Mono<Void> rejectByQuota(ServerWebExchange exchange,
                                     RateLimitRule rule,
                                     RateLimitDecision decision) {
        applyRateLimitHeaders(exchange, rule, decision);
        long retryAfterSeconds = retryAfterSeconds(decision.retryAfterMs(), decision.resetAfterMs());
        exchange.getResponse().getHeaders().set(
                GatewayRateLimitConstants.RETRY_AFTER_HEADER, Long.toString(retryAfterSeconds));
        log.warn("Gateway rate limit rejected request, requestId={}, scope={}, algorithm={}, retryAfterSeconds={}",
                requestId(exchange), rule.scope(), rule.algorithm(), retryAfterSeconds);
        return GatewayFilterSupport.writeError(exchange, HttpStatus.TOO_MANY_REQUESTS,
                "Too many requests");
    }

    private Mono<Void> handleUnavailable(ServerWebExchange exchange,
                                         GatewayFilterChain chain,
                                         Throwable error) {
        String requestId = requestId(exchange);
        if (properties.getFailureMode() == GatewayRateLimitProperties.FailureMode.FAIL_OPEN) {
            metrics.recordPolicy("fail-open");
            log.warn("Gateway rate limit unavailable; request allowed, requestId={}, reason={}",
                    requestId, error.getClass().getSimpleName());
            return chain.filter(exchange);
        }
        metrics.recordPolicy("fail-closed");
        log.error("Gateway rate limit unavailable; request rejected, requestId={}, reason={}",
                requestId, error.getClass().getSimpleName());
        return GatewayFilterSupport.writeError(exchange, HttpStatus.SERVICE_UNAVAILABLE,
                "Rate limit service unavailable");
    }

    private void applyRateLimitHeaders(ServerWebExchange exchange,
                                       RateLimitRule rule,
                                       RateLimitDecision decision) {
        var headers = exchange.getResponse().getHeaders();
        headers.set(GatewayRateLimitConstants.RATE_LIMIT_LIMIT_HEADER, Long.toString(decision.limit()));
        headers.set(GatewayRateLimitConstants.RATE_LIMIT_REMAINING_HEADER,
                Long.toString(Math.max(0L, decision.remaining())));
        headers.set(GatewayRateLimitConstants.RATE_LIMIT_RESET_HEADER,
                Long.toString(toSeconds(decision.resetAfterMs())));
    }

    private long retryAfterSeconds(long retryAfterMs, long resetAfterMs) {
        long millis = retryAfterMs > 0 ? retryAfterMs : resetAfterMs;
        return Math.max(1L, toSeconds(millis));
    }

    private long toSeconds(long millis) {
        if (millis <= 0) {
            return 0L;
        }
        return Math.max(1L, (millis + 999L) / 1000L);
    }

    private String requestId(ServerWebExchange exchange) {
        String requestId = exchange.getRequest().getHeaders().getFirst(GatewayConstant.REQUEST_ID_HEADER);
        return requestId == null ? "" : requestId;
    }

    @Override
    public int getOrder() {
        return FILTER_ORDER;
    }
}
