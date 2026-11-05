package com.peach.gateway.core.ratelimit;

import com.peach.gateway.core.ratelimit.config.GatewayRateLimitProperties;
import com.peach.gateway.core.ratelimit.model.RateLimitAlgorithm;
import com.peach.gateway.core.ratelimit.model.RateLimitScope;
import com.peach.gateway.core.ratelimit.script.RateLimitScriptRepository;
import com.peach.gateway.core.ratelimit.service.RateLimitRuleRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 多算法限流基础能力测试。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/05
 */
class RateLimitFoundationTest {

    @Test
    void shouldLoadEveryLocalLuaScript() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        RateLimitScriptRepository repository = new RateLimitScriptRepository(
                properties, new DefaultResourceLoader());

        repository.reload().block(Duration.ofSeconds(10));

        for (RateLimitAlgorithm algorithm : RateLimitAlgorithm.values()) {
            assertThat(repository.get(algorithm))
                    .as("Lua script for %s", algorithm)
                    .isNotBlank()
                    .contains("redis.call");
        }
    }

    @Test
    void shouldAllowDifferentAlgorithmsForDifferentScopes() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setRules(List.of(
                rule("global-token", RateLimitScope.GLOBAL, RateLimitAlgorithm.TOKEN_BUCKET),
                rule("ip-fixed", RateLimitScope.IP, RateLimitAlgorithm.FIXED_WINDOW),
                uriRule("uri-log", RateLimitAlgorithm.SLIDING_WINDOW_LOG),
                rule("tenant-gcra", RateLimitScope.TENANT, RateLimitAlgorithm.GCRA)));

        RateLimitRuleRegistry registry = new RateLimitRuleRegistry(properties);

        assertThat(registry.getRules())
                .extracting(rule -> rule.algorithm())
                .containsExactly(
                        RateLimitAlgorithm.TOKEN_BUCKET,
                        RateLimitAlgorithm.FIXED_WINDOW,
                        RateLimitAlgorithm.SLIDING_WINDOW_LOG,
                        RateLimitAlgorithm.GCRA);
    }

    @Test
    void shouldRejectImpossibleTokenBucketCost() {
        GatewayRateLimitProperties.RuleProperties rule =
                rule("invalid-token", RateLimitScope.GLOBAL, RateLimitAlgorithm.TOKEN_BUCKET);
        rule.setBurstCapacity(2);
        rule.setCost(3);

        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setRules(List.of(rule));

        assertThatThrownBy(() -> new RateLimitRuleRegistry(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid rate, capacity or cost");
    }

    private GatewayRateLimitProperties.RuleProperties rule(
            String id, RateLimitScope scope, RateLimitAlgorithm algorithm) {
        GatewayRateLimitProperties.RuleProperties rule = new GatewayRateLimitProperties.RuleProperties();
        rule.setId(id);
        rule.setScope(scope);
        rule.setAlgorithm(algorithm);
        return rule;
    }

    private GatewayRateLimitProperties.RuleProperties uriRule(String id, RateLimitAlgorithm algorithm) {
        GatewayRateLimitProperties.RuleProperties rule = rule(id, RateLimitScope.URI, algorithm);
        rule.setPaths(List.of("/api/orders/**"));
        return rule;
    }
}
