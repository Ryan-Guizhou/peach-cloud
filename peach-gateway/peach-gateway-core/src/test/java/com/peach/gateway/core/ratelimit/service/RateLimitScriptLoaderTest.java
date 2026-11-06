package com.peach.gateway.core.ratelimit.service;

import com.peach.gateway.core.ratelimit.config.GatewayRateLimitProperties;
import com.peach.gateway.core.ratelimit.script.RateLimitScriptRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import reactor.core.publisher.Mono;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
    void shouldRecordSuccessfulReloadWhenSnapshotIsPublished() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setEnabled(false);
        RateLimitRuleRegistry registry = new RateLimitRuleRegistry(properties);
        RateLimitScriptRepository repository = mock(RateLimitScriptRepository.class);
        RateLimitMetrics metrics = mock(RateLimitMetrics.class);
        when(repository.reload()).thenReturn(Mono.just(true));
        when(repository.isReady()).thenReturn(true);

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(RateLimitRuleRegistry.class, () -> registry);
            context.registerBean(RateLimitScriptLoader.class,
                    () -> new RateLimitScriptLoader(registry, repository, metrics));
            context.refresh();

            properties.setEnabled(true);
            context.publishEvent(new EnvironmentChangeEvent(
                    Set.of("peach.gateway.rate-limit.enabled")));
            context.publishEvent(new RefreshScopeRefreshedEvent("rate-limit-script-loader-test"));

            verify(repository).reload();
            verify(metrics, org.mockito.Mockito.times(2)).updateScriptReady(true);
        verify(metrics).recordScriptReload(true);
        }
    }
    @Test
    void shouldNotRecordSupersededReloadAsSuccessful() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setEnabled(false);
        RateLimitRuleRegistry registry = new RateLimitRuleRegistry(properties);
        RateLimitScriptRepository repository = mock(RateLimitScriptRepository.class);
        when(repository.reload()).thenReturn(Mono.just(false));
        when(repository.isReady()).thenReturn(false);
        RateLimitMetrics metrics = mock(RateLimitMetrics.class);

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(RateLimitRuleRegistry.class, () -> registry);
            context.registerBean(RateLimitScriptLoader.class,
                    () -> new RateLimitScriptLoader(registry, repository, metrics));
            context.refresh();

            properties.setEnabled(true);
            context.publishEvent(new EnvironmentChangeEvent(Set.of("peach.gateway.rate-limit.enabled")));
            context.publishEvent(new RefreshScopeRefreshedEvent("rate-limit-superseded-test"));

            verify(repository).reload();
            verify(metrics, org.mockito.Mockito.times(2)).updateScriptReady(false);
            verify(metrics, never()).recordScriptReload(true);
            verify(metrics, never()).recordScriptReload(false);
        }
    }

    @Test
    void shouldKeepApplicationStartupWhenInitialScriptLoadFails() throws Exception {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setEnabled(true);
        RateLimitRuleRegistry registry = new RateLimitRuleRegistry(properties);
        RateLimitScriptRepository repository = mock(RateLimitScriptRepository.class);
        when(repository.reload()).thenReturn(Mono.error(new IllegalStateException("OSS unavailable")));
        when(repository.isReady()).thenReturn(false);
        RateLimitMetrics metrics = mock(RateLimitMetrics.class);
        RateLimitScriptLoader loader = new RateLimitScriptLoader(registry, repository, metrics);

        assertThatCode(() -> loader.run(new DefaultApplicationArguments(new String[0])))
                .doesNotThrowAnyException();

        verify(metrics).updateScriptReady(false);
        verify(metrics).recordScriptReload(false);
    }

    @Test
    void shouldRegisterNotReadyGaugeWhenRateLimitingIsDisabledAtStartup() throws Exception {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setEnabled(false);
        RateLimitRuleRegistry registry = new RateLimitRuleRegistry(properties);
        RateLimitScriptRepository repository = mock(RateLimitScriptRepository.class);
        when(repository.isReady()).thenReturn(false);
        RateLimitMetrics metrics = mock(RateLimitMetrics.class);
        RateLimitScriptLoader loader = new RateLimitScriptLoader(registry, repository, metrics);

        loader.run(new DefaultApplicationArguments(new String[0]));

        verify(metrics).updateScriptReady(false);
            verify(repository, never()).reload();
    }

    @Test
    void shouldRefreshReadinessOnConfigurationChangesWhileDisabled() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setEnabled(false);
        RateLimitRuleRegistry registry = new RateLimitRuleRegistry(properties);
        RateLimitScriptRepository repository = mock(RateLimitScriptRepository.class);
        when(repository.isReady()).thenReturn(false);
        RateLimitMetrics metrics = mock(RateLimitMetrics.class);

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(RateLimitRuleRegistry.class, () -> registry);
            context.registerBean(RateLimitScriptLoader.class,
                    () -> new RateLimitScriptLoader(registry, repository, metrics));
            context.refresh();

            context.publishEvent(new EnvironmentChangeEvent(Set.of(
                    "peach.gateway.rate-limit.scripts.local-location")));
            context.publishEvent(new RefreshScopeRefreshedEvent("rate-limit-disabled-refresh-test"));

            verify(metrics).updateScriptReady(false);
            verify(repository, never()).reload();
        }
    }

    @Test
    void shouldRecordFailedReloadAndPreservePreviousReadiness() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setEnabled(false);
        RateLimitRuleRegistry registry = new RateLimitRuleRegistry(properties);
        RateLimitScriptRepository repository = mock(RateLimitScriptRepository.class);
        when(repository.reload()).thenReturn(Mono.error(new IllegalStateException("invalid script")));
        when(repository.isReady()).thenReturn(true);
        RateLimitMetrics metrics = mock(RateLimitMetrics.class);

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(RateLimitRuleRegistry.class, () -> registry);
            context.registerBean(RateLimitScriptLoader.class,
                    () -> new RateLimitScriptLoader(registry, repository, metrics));
            context.refresh();

            properties.setEnabled(true);
            context.publishEvent(new EnvironmentChangeEvent(Set.of("peach.gateway.rate-limit.enabled")));
            context.publishEvent(new RefreshScopeRefreshedEvent("rate-limit-script-failure-test"));

            verify(repository).reload();
            verify(metrics, org.mockito.Mockito.times(2)).updateScriptReady(true);
            verify(metrics).recordScriptReload(false);
        }
    }

}
