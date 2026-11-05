package com.peach.gateway.core.ratelimit.service;

import com.peach.gateway.core.ratelimit.config.GatewayRateLimitProperties;
import com.peach.gateway.core.ratelimit.model.MatchedRateLimitRule;
import com.peach.gateway.core.ratelimit.model.RateLimitCheckResult;
import com.peach.gateway.core.ratelimit.model.RateLimitConfigurationSnapshot;
import com.peach.gateway.core.ratelimit.model.RateLimitDecision;
import com.peach.gateway.core.ratelimit.model.RateLimitRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 限流规则执行编排器。
 *
 * <p>按优先级顺序执行所有匹配规则，首个拒绝结果立即短路。不同 Redis Key 不构成分布式事务，
 * 因此前序规则已扣减的额度不会因后续规则拒绝而回滚。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/05
 */
@Component
@RequiredArgsConstructor
public class RateLimitEnforcementService {

    private final GatewayRateLimitProperties properties;
    private final RateLimitRuleRegistry ruleRegistry;
    private final RateLimitRuleMatcher ruleMatcher;
    private final RateLimitKeyFactory keyFactory;
    private final RedisRateLimitExecutor redisExecutor;
    private final TrustedProxyClientIpResolver clientIpResolver;
    private final RateLimitTenantContextResolver tenantContextResolver;
    private final RateLimitMetrics metrics;

    public Mono<RateLimitCheckResult> check(ServerWebExchange exchange) {
        return Mono.defer(() -> {
            if (!properties.isEnabled()) {
                return Mono.just(RateLimitCheckResult.allowed());
            }

            RateLimitConfigurationSnapshot configuration = ruleRegistry.getSnapshot();
            String clientIp = clientIpResolver.resolve(exchange);

            if (configuration.denylistIps().contains(clientIp)) {
                metrics.recordPolicy("blocked");
                return Mono.just(RateLimitCheckResult.blocked("ip-denylist"));
            }
            if (configuration.allowlistIps().contains(clientIp)) {
                metrics.recordPolicy("allowlisted");
                return Mono.just(RateLimitCheckResult.allowed());
            }

            List<RateLimitRule> candidates = configuration.rules().stream()
                    .filter(rule -> ruleMatcher.matchesRequest(rule, exchange))
                    .toList();
            if (candidates.isEmpty()) {
                return Mono.just(RateLimitCheckResult.allowed());
            }

            boolean tenantScopeMatched = candidates.stream().anyMatch(rule -> rule.scope().isTenantScope());
            if (!tenantScopeMatched) {
                return evaluate(candidates, exchange, null, clientIp);
            }

            return tenantContextResolver.resolve()
                    .defaultIfEmpty("")
                    .flatMap(tenantId -> {
                        if (isBlank(tenantId) && properties.isRequireTenantContext()) {
                            metrics.recordPolicy("tenant-context-missing");
                            return Mono.just(RateLimitCheckResult.tenantContextMissing());
                        }
                        if (!isBlank(tenantId) && configuration.denylistTenants().contains(tenantId)) {
                            metrics.recordPolicy("blocked");
                            return Mono.just(RateLimitCheckResult.blocked("tenant-denylist"));
                        }
                        if (!isBlank(tenantId) && configuration.allowlistTenants().contains(tenantId)) {
                            metrics.recordPolicy("allowlisted");
                            return Mono.just(RateLimitCheckResult.allowed());
                        }
                        return evaluate(candidates, exchange, tenantId, clientIp);
                    });
        });
    }

    private Mono<RateLimitCheckResult> evaluate(List<RateLimitRule> candidates,
                                                ServerWebExchange exchange,
                                                String tenantId,
                                                String clientIp) {
        List<MatchedRateLimitRule> matchedRules = new ArrayList<>();
        for (RateLimitRule rule : candidates) {
            if (rule.scope().isIpScope() && (isBlank(clientIp) || "unknown".equalsIgnoreCase(clientIp))) {
                metrics.recordPolicy("client-ip-unavailable");
                return Mono.just(RateLimitCheckResult.unavailable("client-ip-unavailable"));
            }
            ruleMatcher.match(rule, exchange, tenantId, clientIp).ifPresent(matchedRules::add);
        }
        if (matchedRules.isEmpty()) {
            return Mono.just(RateLimitCheckResult.allowed());
        }

        return Flux.fromIterable(matchedRules)
                .concatMap(matched -> executeRule(matched, tenantId, clientIp, exchange))
                .takeUntil(result -> result.status() != RateLimitCheckResult.Status.ALLOWED)
                .collectList()
                .map(this::selectResult);
    }

    private Mono<RateLimitCheckResult> executeRule(MatchedRateLimitRule matched,
                                                   String tenantId,
                                                   String clientIp,
                                                   ServerWebExchange exchange) {
        RateLimitRule rule = matched.rule();
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getURI().getRawPath();
        String key = keyFactory.create(matched, clientIp, tenantId, method, path);
        long startNanos = System.nanoTime();
        return redisExecutor.execute(rule, key)
                .doOnNext(decision -> metrics.record(rule,
                        decision.allowed() ? "allowed" : "rejected",
                        System.nanoTime() - startNanos))
                .map(decision -> decision.allowed()
                        ? RateLimitCheckResult.allowed(rule, decision)
                        : RateLimitCheckResult.rejected(rule, decision))
                .doOnError(error -> metrics.record(rule, "error", System.nanoTime() - startNanos));
    }

    private RateLimitCheckResult selectResult(List<RateLimitCheckResult> results) {
        return results.stream()
                .filter(result -> result.status() != RateLimitCheckResult.Status.ALLOWED)
                .findFirst()
                .orElseGet(() -> results.stream()
                        .filter(result -> result.rule() != null && result.decision() != null)
                        .min(Comparator.comparingDouble(this::remainingRatio))
                        .orElseGet(RateLimitCheckResult::allowed));
    }

    private double remainingRatio(RateLimitCheckResult result) {
        RateLimitDecision decision = result.decision();
        if (decision.limit() <= 0) {
            return 1D;
        }
        return (double) Math.max(0L, decision.remaining()) / decision.limit();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
