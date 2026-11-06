package com.peach.gateway.core.ratelimit.service;

import com.peach.gateway.core.ratelimit.config.GatewayRateLimitProperties;
import com.peach.gateway.core.ratelimit.model.RateLimitAlgorithm;
import com.peach.gateway.core.ratelimit.model.RateLimitScope;
import com.peach.gateway.core.ratelimit.model.RateLimitConfigurationSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.Set;

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


    @Test
    void shouldCanonicalizeIpv6IpPolicies() throws Exception {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setAllowlistIps(List.of("2001:db8::1"));
        properties.setDenylistIps(List.of("2001:db8::2"));

        RateLimitRuleRegistry registry = new RateLimitRuleRegistry(properties);

        assertThat(registry.getSnapshot().allowlistIps())
                .containsExactly(InetAddress.getByName("2001:db8::1").getHostAddress());
        assertThat(registry.getSnapshot().denylistIps())
                .containsExactly(InetAddress.getByName("2001:db8::2").getHostAddress());
    }

    @Test
    void shouldCaptureAllRequestExecutionSettingsInTheRuntimeSnapshot() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setEnabled(true);
        properties.setFailureMode(GatewayRateLimitProperties.FailureMode.FAIL_OPEN);
        properties.setRequireTenantContext(false);
        properties.setMaxMatchedRulesPerRequest(16);
        properties.setTrustedProxies(List.of("10.0.0.0/8"));

        RateLimitConfigurationSnapshot snapshot = new RateLimitRuleRegistry(properties).getSnapshot();

        assertThat(snapshot.enabled()).isTrue();
        assertThat(snapshot.failureMode()).isEqualTo(GatewayRateLimitProperties.FailureMode.FAIL_OPEN);
        assertThat(snapshot.requireTenantContext()).isFalse();
        assertThat(snapshot.maxMatchedRulesPerRequest()).isEqualTo(16);
        assertThat(snapshot.trustedProxies()).containsExactly("10.0.0.0/8");
    }

    @Test
    void shouldCaptureRemoteScriptSecuritySettingsInTheRuntimeSnapshot() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.getScripts().setAllowedRemoteHosts(List.of("cdn.example.com"));
        properties.getScripts().setAllowedRemoteCidrs(List.of("10.20.0.0/16"));
        properties.getScripts().setAllowedRemotePorts(List.of(443, 8443));

        RateLimitConfigurationSnapshot snapshot = new RateLimitRuleRegistry(properties).getSnapshot();

        assertThat(snapshot.scripts().requireHttps()).isTrue();
        assertThat(snapshot.scripts().allowedRemoteHosts()).containsExactly("cdn.example.com");
        assertThat(snapshot.scripts().allowedRemoteCidrs()).containsExactly("10.20.0.0/16");
        assertThat(snapshot.scripts().allowedRemotePorts()).containsExactlyInAnyOrder(443, 8443);
    }

    @Test
    void shouldRetainPreviousSnapshotWhenRemoteScriptPortConfigurationIsInvalid() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        RateLimitRuleRegistry registry = new RateLimitRuleRegistry(properties);
        RateLimitConfigurationSnapshot previous = registry.getSnapshot();

        properties.getScripts().setAllowedRemotePorts(List.of(0));

        assertThatThrownBy(registry::refresh)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Remote rate limit script ports");
        assertThat(registry.getSnapshot()).isSameAs(previous);
    }

    @Test
    void shouldRejectBroadRemoteScriptCidrsWithoutReplacingTheRuntimeSnapshot() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        RateLimitRuleRegistry registry = new RateLimitRuleRegistry(properties);
        RateLimitConfigurationSnapshot previous = registry.getSnapshot();
        properties.getScripts().setAllowedRemoteCidrs(List.of("0.0.0.0/0"));

        assertThatThrownBy(registry::refresh)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("authorized CIDR");
        assertThat(registry.getSnapshot()).isSameAs(previous);
    }

    @Test
    void shouldRequireAnExactHostAllowlistWhenRemoteScriptsAreConfigured() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        RateLimitRuleRegistry registry = new RateLimitRuleRegistry(properties);
        RateLimitConfigurationSnapshot previous = registry.getSnapshot();
        properties.getScripts().setRemoteBaseUrl("https://cdn.example.com/lua/");

        assertThatThrownBy(registry::refresh)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("allowed-remote-hosts");
        assertThat(registry.getSnapshot()).isSameAs(previous);
    }

    @Test
    void shouldRetainPreviousSnapshotWhenFailureModeIsMissing() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        RateLimitRuleRegistry registry = new RateLimitRuleRegistry(properties);
        RateLimitConfigurationSnapshot previous = registry.getSnapshot();
        properties.setFailureMode(null);

        assertThatThrownBy(registry::refresh)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("failure-mode");
        assertThat(registry.getSnapshot()).isSameAs(previous);
    }

    @Test
    void shouldRequireSha256ForEachConfiguredRemoteScript() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.getScripts().setAllowedRemoteHosts(List.of("1.1.1.1"));
        properties.getScripts().setRemoteUrls(java.util.Map.of(
                "token-bucket", "https://1.1.1.1/rate-limit/token-bucket.lua"));

        assertThatThrownBy(() -> new RateLimitRuleRegistry(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires a SHA-256 checksum");
    }

    @Test
    void shouldCountOnlyEnabledRulesAgainstMaximum() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setMaxRules(1);
        GatewayRateLimitProperties.RuleProperties active = rule("active", RateLimitScope.GLOBAL);
        GatewayRateLimitProperties.RuleProperties disabledOne = rule("disabled-one", RateLimitScope.IP);
        GatewayRateLimitProperties.RuleProperties disabledTwo = rule("disabled-two", RateLimitScope.URI);
        disabledOne.setEnabled(false);
        disabledTwo.setEnabled(false);
        properties.setRules(List.of(active, disabledOne, disabledTwo));

        RateLimitRuleRegistry registry = new RateLimitRuleRegistry(properties);

        assertThat(registry.getSnapshot().rules()).hasSize(1);
        assertThat(registry.getSnapshot().rules().get(0).id()).isEqualTo("active");
    }

    @Test
    void shouldRefreshAfterRefreshScopeEventAndKeepPreviousSnapshotWhenInvalid() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setRules(List.of(rule("global", RateLimitScope.GLOBAL)));

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(RateLimitRuleRegistry.class, () -> new RateLimitRuleRegistry(properties));
            context.refresh();

            RateLimitRuleRegistry registry = context.getBean(RateLimitRuleRegistry.class);
            properties.setAllowlistIps(List.of("192.0.2.8"));
            context.publishEvent(new EnvironmentChangeEvent(Set.of("peach.gateway.rate-limit.allowlist-ips")));
            var previous = registry.getSnapshot();
            assertThat(previous.allowlistIps()).isEmpty();

            context.publishEvent(new RefreshScopeRefreshedEvent("rate-limit-registry-test"));
            var refreshed = registry.getSnapshot();
            assertThat(refreshed.allowlistIps()).containsExactly("192.0.2.8");

            properties.setRules(List.of(
                    rule("duplicate", RateLimitScope.GLOBAL),
                    rule("duplicate", RateLimitScope.IP)));
            context.publishEvent(new EnvironmentChangeEvent(Set.of("peach.gateway.rate-limit.rules")));
            context.publishEvent(new RefreshScopeRefreshedEvent("rate-limit-registry-invalid-test"));

            assertThat(registry.getSnapshot()).isSameAs(refreshed);
            assertThat(registry.getSnapshot().rules())
                    .extracting(com.peach.gateway.core.ratelimit.model.RateLimitRule::id)
                    .containsExactly("global");
        }
    }

    @Test
    void shouldRejectSlidingWindowLogCostAboveSafetyCap() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        GatewayRateLimitProperties.RuleProperties rule = rule("login", RateLimitScope.IP);
        rule.setAlgorithm(RateLimitAlgorithm.SLIDING_WINDOW_LOG);
        rule.setLimit(10_000);
        rule.setCost(101);
        rule.setWindow(Duration.ofSeconds(60));
        properties.setRules(List.of(rule));

        assertThatThrownBy(() -> new RateLimitRuleRegistry(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sliding window log");
    }

    @Test
    void shouldRejectInvalidIpPolicyAddress() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setDenylistIps(List.of("not-an-ip"));

        assertThatThrownBy(() -> new RateLimitRuleRegistry(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid IP address");
    }

    private GatewayRateLimitProperties.RuleProperties rule(String id, RateLimitScope scope) {
        GatewayRateLimitProperties.RuleProperties rule = new GatewayRateLimitProperties.RuleProperties();
        rule.setId(id);
        rule.setScope(scope);
        rule.setAlgorithm(RateLimitAlgorithm.TOKEN_BUCKET);
        return rule;
    }
}
