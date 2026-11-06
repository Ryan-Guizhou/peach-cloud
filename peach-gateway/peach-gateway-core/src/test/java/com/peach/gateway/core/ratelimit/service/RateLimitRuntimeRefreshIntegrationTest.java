package com.peach.gateway.core.ratelimit.service;

import com.peach.gateway.core.ratelimit.config.GatewayRateLimitProperties;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 限流配置刷新生命周期集成测试。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/06
 */
class RateLimitRuntimeRefreshIntegrationTest {

    @Test
    void shouldPublishNewRuntimeSnapshotOnlyAfterConfigurationRebindingCompletes() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setAllowlistIps(List.of("192.0.2.10"));

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(RateLimitRuleRegistry.class, () -> new RateLimitRuleRegistry(properties));
            context.refresh();

            RateLimitRuleRegistry registry = context.getBean(RateLimitRuleRegistry.class);
            var previous = registry.getSnapshot();

            properties.setAllowlistIps(List.of("192.0.2.20"));
            context.publishEvent(new EnvironmentChangeEvent(
                    Set.of("peach.gateway.rate-limit.allowlist-ips")));

            assertThat(registry.getSnapshot())
                    .as("EnvironmentChangeEvent is emitted before ConfigurationProperties rebinding finishes")
                    .isSameAs(previous);

            context.publishEvent(new RefreshScopeRefreshedEvent("rate-limit-refresh-test"));

            assertThat(registry.getSnapshot()).isNotSameAs(previous);
            assertThat(registry.getSnapshot().allowlistIps()).containsExactly("192.0.2.20");
        }
    }
}
