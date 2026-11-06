package com.peach.gateway.core.ratelimit.service;

import com.peach.gateway.core.config.GatewaySaTokenProperties;
import com.peach.gateway.core.ratelimit.config.GatewayRateLimitProperties;
import com.peach.gateway.core.ratelimit.model.MatchedRateLimitRule;
import com.peach.gateway.core.ratelimit.model.RateLimitCheckResult;
import com.peach.gateway.core.ratelimit.model.RateLimitConfigurationSnapshot;
import com.peach.gateway.core.ratelimit.model.RateLimitDecision;
import com.peach.gateway.core.ratelimit.model.RateLimitAlgorithm;
import com.peach.gateway.core.ratelimit.model.RateLimitRule;
import com.peach.gateway.core.ratelimit.script.RateLimitScriptRepository;
import com.peach.gateway.core.security.GatewaySecurityEndpointMatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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

    private final RateLimitRuleRegistry ruleRegistry;
    private final RateLimitRuleMatcher ruleMatcher;
    private final RateLimitKeyFactory keyFactory;
    private final RedisRateLimitExecutor redisExecutor;
    private final TrustedProxyClientIpResolver clientIpResolver;
    private final RateLimitTenantContextResolver tenantContextResolver;
    private final RateLimitMetrics metrics;
    private final RateLimitScriptRepository scriptRepository;
    private final GatewaySaTokenProperties saTokenProperties;
    private static final String CONFIGURATION_ATTRIBUTE =
            RateLimitEnforcementService.class.getName() + ".configuration";

    private final GatewaySecurityEndpointMatcher endpointMatcher = new GatewaySecurityEndpointMatcher();

    public Mono<RateLimitCheckResult> check(ServerWebExchange exchange) {
        return Mono.defer(() -> {
            RateLimitConfigurationSnapshot configuration = ruleRegistry.getSnapshot();
            exchange.getAttributes().put(CONFIGURATION_ATTRIBUTE, configuration);
            if (!configuration.enabled()) {
                return Mono.just(RateLimitCheckResult.allowed());
            }

            String clientIp = clientIpResolver.resolve(exchange, configuration);

            if (clientIp != null && configuration.denylistIps().contains(clientIp)) {
                metrics.recordPolicy("blocked");
                return Mono.just(RateLimitCheckResult.blocked("ip-denylist"));
            }
            boolean ipAllowlisted = clientIp != null && configuration.allowlistIps().contains(clientIp);
            List<RateLimitRule> matchedCandidates = configuration.rules().stream()
                    .filter(rule -> ruleMatcher.matchesRequest(rule, exchange))
                    .toList();
            boolean publicEndpoint = isPublicEndpoint(exchange);
            List<RateLimitRule> candidates = publicEndpoint
                    ? matchedCandidates.stream().filter(rule -> !rule.scope().isTenantScope()).toList()
                    : matchedCandidates;
            boolean tenantScopeMatched = !publicEndpoint
                    && candidates.stream().anyMatch(rule -> rule.scope().isTenantScope());
            boolean tenantPolicyConfigured = !publicEndpoint && (!configuration.allowlistTenants().isEmpty()
                    || !configuration.denylistTenants().isEmpty());
            Map<RateLimitAlgorithm, String> requestScripts = scriptRepository.getScripts(configuration.scripts());

            if (candidates.isEmpty() && !tenantPolicyConfigured) {
                if (ipAllowlisted) {
                    metrics.recordPolicy("allowlisted");
                }
                return Mono.just(RateLimitCheckResult.allowed());
            }
            if (!tenantScopeMatched && !tenantPolicyConfigured) {
                if (ipAllowlisted) {
                    metrics.recordPolicy("allowlisted");
                    return Mono.just(RateLimitCheckResult.allowed());
                }
                return evaluate(candidates, exchange, null, clientIp, configuration, requestScripts);
            }

            return tenantContextResolver.resolve(exchange)
                    .defaultIfEmpty("")
                    .flatMap(tenantId -> {
                        if (!isBlank(tenantId) && configuration.denylistTenants().contains(tenantId)) {
                            metrics.recordPolicy("blocked");
                            return Mono.just(RateLimitCheckResult.blocked("tenant-denylist"));
                        }
                        if (ipAllowlisted
                                || (!isBlank(tenantId) && configuration.allowlistTenants().contains(tenantId))) {
                            metrics.recordPolicy("allowlisted");
                            return Mono.just(RateLimitCheckResult.allowed());
                        }
                        if (tenantScopeMatched && isBlank(tenantId) && configuration.requireTenantContext()) {
                            metrics.recordPolicy("tenant-context-missing");
                            return Mono.just(RateLimitCheckResult.tenantContextMissing());
                        }
                        if (candidates.isEmpty()) {
                            return Mono.just(RateLimitCheckResult.allowed());
                        }
                        return evaluate(candidates, exchange, tenantId, clientIp, configuration, requestScripts);
                    });
        });
    }

    private Mono<RateLimitCheckResult> evaluate(List<RateLimitRule> candidates,
                                                ServerWebExchange exchange,
                                                String tenantId,
                                                String clientIp,
                                                RateLimitConfigurationSnapshot configuration,
                                                Map<RateLimitAlgorithm, String> requestScripts) {
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
        if (requestScripts.size() != RateLimitAlgorithm.values().length) {
            return unavailableWhileScriptsNotReady();
        }
        if (matchedRules.size() > configuration.maxMatchedRulesPerRequest()) {
            metrics.recordPolicy("matched-rules-exceeded");
            return Mono.just(RateLimitCheckResult.unavailable("matched-rules-exceeded"));
        }

        return Flux.fromIterable(matchedRules)
                .concatMap(matched -> executeRule(matched, tenantId, clientIp, exchange, requestScripts))
                .takeUntil(result -> result.status() != RateLimitCheckResult.Status.ALLOWED)
                .collectList()
                .map(this::selectResult);
    }

    private Mono<RateLimitCheckResult> executeRule(MatchedRateLimitRule matched,
                                                   String tenantId,
                                                   String clientIp,
                                                   ServerWebExchange exchange,
                                                   Map<RateLimitAlgorithm, String> requestScripts) {
        RateLimitRule rule = matched.rule();
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getURI().getRawPath();
        String key = keyFactory.create(matched, clientIp, tenantId, method, path);
        long startNanos = System.nanoTime();
        return redisExecutor.execute(rule, key, requestScripts.get(rule.algorithm()))
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

    private boolean isPublicEndpoint(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getRawPath();
        String method = exchange.getRequest().getMethod().name();
        return endpointMatcher.matches(saTokenProperties.getPublicEndpoints(), method, path);
    }

    public boolean isEnabled() {
        return ruleRegistry.getSnapshot().enabled();
    }

    public GatewayRateLimitProperties.FailureMode failureMode(ServerWebExchange exchange) {
        Object configuration = exchange.getAttributes().get(CONFIGURATION_ATTRIBUTE);
        if (configuration instanceof RateLimitConfigurationSnapshot snapshot) {
            return snapshot.failureMode();
        }
        return ruleRegistry.getSnapshot().failureMode();
    }

    private Mono<RateLimitCheckResult> unavailableWhileScriptsNotReady() {
        metrics.recordPolicy("scripts-not-ready");
        return Mono.just(RateLimitCheckResult.unavailable("rate-limit-scripts-not-ready"));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
