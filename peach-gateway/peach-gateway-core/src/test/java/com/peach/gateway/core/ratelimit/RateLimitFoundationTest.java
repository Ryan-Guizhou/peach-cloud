package com.peach.gateway.core.ratelimit;

import com.peach.gateway.core.ratelimit.config.GatewayRateLimitProperties;
import com.peach.gateway.core.ratelimit.model.MatchedRateLimitRule;
import com.peach.gateway.core.ratelimit.model.RateLimitAlgorithm;
import com.peach.gateway.core.ratelimit.model.RateLimitRule;
import com.peach.gateway.core.ratelimit.model.RateLimitScope;
import com.peach.gateway.core.ratelimit.script.RateLimitScriptRepository;
import com.peach.gateway.core.ratelimit.service.RateLimitKeyFactory;
import com.peach.gateway.core.ratelimit.service.RateLimitRuleMatcher;
import com.peach.gateway.core.ratelimit.service.RateLimitRuleRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.reactive.MockServerWebExchange;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

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
                .extracting(RateLimitRule::algorithm)
                .containsExactly(
                        RateLimitAlgorithm.TOKEN_BUCKET,
                        RateLimitAlgorithm.FIXED_WINDOW,
                        RateLimitAlgorithm.SLIDING_WINDOW_LOG,
                        RateLimitAlgorithm.GCRA);
    }

    @Test
    void shouldRejectDuplicateRuleIds() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setRules(List.of(
                rule("duplicate", RateLimitScope.GLOBAL, RateLimitAlgorithm.TOKEN_BUCKET),
                rule("duplicate", RateLimitScope.IP, RateLimitAlgorithm.FIXED_WINDOW)));

        assertThatThrownBy(() -> new RateLimitRuleRegistry(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate rate limit rule id");
    }

    @Test
    void shouldRejectWindowThatTruncatesToZeroMilliseconds() {
        GatewayRateLimitProperties.RuleProperties rule =
                rule("short-window", RateLimitScope.GLOBAL, RateLimitAlgorithm.FIXED_WINDOW);
        rule.setWindow(Duration.ofNanos(500_000));

        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setRules(List.of(rule));

        assertThatThrownBy(() -> new RateLimitRuleRegistry(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outside supported range");
    }

    @Test
    void shouldRejectWeightedCostGreaterThanGcraBurst() {
        GatewayRateLimitProperties.RuleProperties rule =
                rule("weighted-gcra", RateLimitScope.GLOBAL, RateLimitAlgorithm.GCRA);
        rule.setBurstCapacity(2);
        rule.setCost(3);

        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setRules(List.of(rule));

        assertThatThrownBy(() -> new RateLimitRuleRegistry(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid rate, capacity or cost");
    }

    @Test
    void shouldApplyPathSelectorsToIpScopeWithoutChangingIpBucketIdentity() {
        GatewayRateLimitProperties.RuleProperties configured =
                rule("login-ip", RateLimitScope.IP, RateLimitAlgorithm.TOKEN_BUCKET);
        configured.setPaths(List.of("/api/auth/login"));
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setRules(List.of(configured));

        RateLimitRule rule = new RateLimitRuleRegistry(properties).getRules().get(0);
        RateLimitRuleMatcher matcher = new RateLimitRuleMatcher();
        MockServerWebExchange loginExchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/auth/login").build());
        MockServerWebExchange otherExchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/auth/register").build());

        MatchedRateLimitRule matched = matcher.match(rule, loginExchange, null, "127.0.0.1").orElseThrow();

        assertThat(matcher.match(rule, otherExchange, null, "127.0.0.1")).isEmpty();
        assertThat(new RateLimitKeyFactory().create(matched, "127.0.0.1", null,
                "POST", "/api/auth/login"))
                .isEqualTo(new RateLimitKeyFactory().create(
                        new MatchedRateLimitRule(rule, "/api/auth/register"),
                        "127.0.0.1", null, "POST", "/api/auth/register"));
    }

    @Test
    void shouldUseLocaleIndependentAlgorithmConfigKeys() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertThat(RateLimitAlgorithm.SLIDING_WINDOW_LOG.getConfigKey())
                    .isEqualTo("sliding-window-log");
        } finally {
            Locale.setDefault(original);
        }
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
