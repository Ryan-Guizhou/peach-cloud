package com.peach.gateway.core.ratelimit.service;

import com.peach.gateway.core.ratelimit.config.GatewayRateLimitProperties;
import com.peach.gateway.core.ratelimit.script.RateLimitScriptRepository;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import reactor.core.publisher.Mono;

import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 限流脚本异步加载生命周期测试。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/06
 */
class RateLimitScriptLoaderTest {

    @Test
    void shouldUpdateReadinessWhenMonoVoidReloadCompletesAfterRefresh() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setEnabled(false);
        RateLimitScriptRepository repository = mock(RateLimitScriptRepository.class);
        RateLimitMetrics metrics = mock(RateLimitMetrics.class);
        when(repository.reload()).thenReturn(Mono.empty());
        when(repository.isReady()).thenReturn(true);

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(RateLimitScriptLoader.class,
                    () -> new RateLimitScriptLoader(properties, repository, metrics));
            context.refresh();

            properties.setEnabled(true);
            context.publishEvent(new EnvironmentChangeEvent(
                    Set.of("peach.gateway.rate-limit.enabled")));
            context.publishEvent(new RefreshScopeRefreshedEvent("rate-limit-script-loader-test"));

            verify(repository).reload();
            verify(metrics).updateScriptReady(true);
        }
    }
}
