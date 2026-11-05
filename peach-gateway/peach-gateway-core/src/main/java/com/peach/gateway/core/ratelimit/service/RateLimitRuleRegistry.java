package com.peach.gateway.core.ratelimit.service;

import com.peach.gateway.core.ratelimit.config.GatewayRateLimitProperties;
import com.peach.gateway.core.ratelimit.model.RateLimitAlgorithm;
import com.peach.gateway.core.ratelimit.model.RateLimitRule;
import com.peach.gateway.core.ratelimit.model.RateLimitScope;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 限流规则注册表。配置刷新采用不可变快照原子替换。
 */
@Component
public class RateLimitRuleRegistry {

    private final GatewayRateLimitProperties properties;
    private final AtomicReference<List<RateLimitRule>> rules = new AtomicReference<>(List.of());

    public RateLimitRuleRegistry(GatewayRateLimitProperties properties) {
        this.properties = properties;
        refresh();
    }

    public List<RateLimitRule> getRules() {
        return rules.get();
    }

    @EventListener(EnvironmentChangeEvent.class)
    public void onEnvironmentChanged(EnvironmentChangeEvent event) {
        refresh();
    }

    public void refresh() {
        List<RateLimitRule> snapshot = properties.getRules().stream()
                .filter(GatewayRateLimitProperties.RuleProperties::isEnabled)
                .map(this::toRule)
                .sorted(Comparator.comparingInt(RateLimitRule::priority))
                .toList();
        rules.set(snapshot);
    }

    private RateLimitRule toRule(GatewayRateLimitProperties.RuleProperties source) {
        if (source.getId() == null || source.getId().isBlank()
                || source.getScope() == null || source.getAlgorithm() == null) {
            throw new IllegalArgumentException("Rate limit rule id, scope and algorithm are required");
        }
        if (source.getCost() <= 0) {
            throw new IllegalArgumentException("Rate limit rule cost must be positive: " + source.getId());
        }
        if (source.getAlgorithm() == RateLimitAlgorithm.TOKEN_BUCKET
                || source.getAlgorithm() == RateLimitAlgorithm.GCRA) {
            if (source.getReplenishRate() <= 0 || source.getBurstCapacity() <= 0) {
                throw new IllegalArgumentException("Invalid rate/burst for rule: " + source.getId());
            }
        }
        if (source.getAlgorithm() == RateLimitAlgorithm.LEAKY_BUCKET
                && (source.getLeakRate() <= 0 || source.getBurstCapacity() <= 0)) {
            throw new IllegalArgumentException("Invalid leak rate/capacity for rule: " + source.getId());
        }
        if ((source.getAlgorithm() == RateLimitAlgorithm.FIXED_WINDOW
                || source.getAlgorithm() == RateLimitAlgorithm.SLIDING_WINDOW_LOG
                || source.getAlgorithm() == RateLimitAlgorithm.SLIDING_WINDOW_COUNTER)
                && (source.getLimit() <= 0 || source.getWindow().isZero() || source.getWindow().isNegative())) {
            throw new IllegalArgumentException("Invalid window/limit for rule: " + source.getId());
        }
        return new RateLimitRule(source.getId(), source.isEnabled(), source.getScope(), source.getAlgorithm(),
                source.getPriority(), Set.copyOf(source.getMethods()), List.copyOf(source.getPaths()),
                Set.copyOf(source.getRouteIds()), source.getLimit(), source.getWindow().toMillis(),
                source.getReplenishRate(), source.getBurstCapacity(), source.getLeakRate(), source.getCost());
    }
}
