package com.peach.gateway.core.ratelimit.service;

import com.peach.gateway.core.ratelimit.config.GatewayRateLimitProperties;
import com.peach.gateway.core.ratelimit.model.RateLimitAlgorithm;
import com.peach.gateway.core.ratelimit.model.RateLimitScope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 限流规则配置快照测试。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/05
 */
class RateLimitRuleRegistryTest {

    @Test
    void shouldPublishRulesAndPolicyListsInOneImmutableSnapshot() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setRules(List.of(rule("global", RateLimitScope.GLOBAL)));
        properties.setAllowlistIps(List.of("192.0.2.10"));
        properties.setDenylistTenants(List.of("tenant-blocked"));

        RateLimitRuleRegistry registry = new RateLimitRuleRegistry(properties);

        assertThat(registry.getSnapshot().rules()).hasSize(1);
        assertThat(registry.getSnapshot().allowlistIps()).containsExactly("192.0.2.10");
        assertThat(registry.getSnapshot().denylistTenants()).containsExactly("tenant-blocked");
        assertThatThrownBy(() -> registry.getSnapshot().allowlistIps().add("192.0.2.11"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldRetainPreviousSnapshotWhenNewConfigurationIsInvalid() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setRules(List.of(rule("global", RateLimitScope.GLOBAL)));
        RateLimitRuleRegistry registry = new RateLimitRuleRegistry(properties);
        var previous = registry.getSnapshot();

        properties.setRules(List.of(
                rule("duplicate", RateLimitScope.GLOBAL),
                rule("duplicate", RateLimitScope.IP)));

        assertThatThrownBy(registry::refresh)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate rate limit rule id");
        assertThat(registry.getSnapshot()).isSameAs(previous);
    }

    private GatewayRateLimitProperties.RuleProperties rule(String id, RateLimitScope scope) {
        GatewayRateLimitProperties.RuleProperties rule = new GatewayRateLimitProperties.RuleProperties();
        rule.setId(id);
        rule.setScope(scope);
        rule.setAlgorithm(RateLimitAlgorithm.TOKEN_BUCKET);
        return rule;
    }
}
