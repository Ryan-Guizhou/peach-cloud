package com.peach.gateway.core.ratelimit.service;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.peach.gateway.core.ratelimit.config.GatewayRateLimitProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ByteArrayResource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 使用真实 Nacos Server 验证限流配置推送和运行时快照刷新生命周期。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/06
 */
@Testcontainers(disabledWithoutDocker = true)
class NacosRateLimitRefreshIntegrationTest {

    private static final String DATA_ID = "gateway-rate-limit-integration.yml";

    private static final String GROUP = "PEACH-CLOUD";

    @Container
    private static final GenericContainer<?> NACOS =
            new GenericContainer<>("nacos/nacos-server:v2.5.2")
                    .withEnv("MODE", "standalone")
                    .withEnv("NACOS_AUTH_ENABLE", "false")
                    .withEnv("JVM_XMS", "256m")
                    .withEnv("JVM_XMX", "512m")
                    .withFixedExposedPort(18848, 8848)
                    .withFixedExposedPort(19848, 9848)
                    .withFixedExposedPort(19849, 9849)
                    .waitingFor(Wait.forHttp("/nacos/v1/console/health/readiness")
                            .forPort(8848)
                            .forStatusCode(200)
                            .withStartupTimeout(Duration.ofMinutes(3)));

    private void publishWithRetry(ConfigService configService, String content) throws Exception {
        boolean published = false;
        for (int attempt = 0; attempt < 10 && !published; attempt++) {
            published = configService.publishConfig(DATA_ID, GROUP, content);
            if (!published) {
                Thread.sleep(1000L);
            }
        }
        assertThat(published).as("Nacos should accept the rate-limit configuration").isTrue();
    }

    @Test
    void shouldReceiveNacosConfigAndPublishValidatedRuntimeSnapshotAfterRebinding() throws Exception {
        Properties clientProperties = new Properties();
        clientProperties.setProperty("serverAddr",
                NACOS.getHost() + ":" + NACOS.getMappedPort(8848));
        clientProperties.setProperty("namespace", "");
        clientProperties.setProperty("username", "nacos");
        clientProperties.setProperty("password", "nacos");
        clientProperties.setProperty("configLongPollTimeout", "3000");
        clientProperties.setProperty("configRetryTime", "1000");

        ConfigService configService = NacosFactory.createConfigService(clientProperties);
        String initialConfig = """
                peach:
                  gateway:
                    rate-limit:
                      enabled: false
                      allowlist-ips: []
                      rules: []
                """;
        String updatedConfig = """
                peach:
                  gateway:
                    rate-limit:
                      enabled: true
                      failure-mode: FAIL_OPEN
                      require-tenant-context: true
                      max-matched-rules-per-request: 7
                      allowlist-ips:
                        - 192.0.2.55
                      scripts:
                        require-https: true
                        allowed-remote-hosts:
                          - cdn.example.com
                        allowed-remote-ports:
                          - 443
                      rules: []
                """;
        publishWithRetry(configService, initialConfig);

        CountDownLatch updateReceived = new CountDownLatch(1);
        AtomicReference<String> pushedConfig = new AtomicReference<>();
        Listener listener = new Listener() {
            @Override
            public java.util.concurrent.Executor getExecutor() {
                return Runnable::run;
            }

            @Override
            public void receiveConfigInfo(String configInfo) {
                if (configInfo != null && configInfo.contains("192.0.2.55")) {
                    pushedConfig.set(configInfo);
                    updateReceived.countDown();
                }
            }
        };

        try {
            configService.addListener(DATA_ID, GROUP, listener);
            publishWithRetry(configService, updatedConfig);
            assertThat(updateReceived.await(30, TimeUnit.SECONDS))
                    .as("Nacos listener should receive the updated configuration")
                    .isTrue();

            GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
            try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
                context.registerBean(RateLimitRuleRegistry.class,
                        () -> new RateLimitRuleRegistry(properties));
                context.refresh();

                RateLimitRuleRegistry registry = context.getBean(RateLimitRuleRegistry.class);
                var previous = registry.getSnapshot();

                StandardEnvironment environment = new StandardEnvironment();
                List<PropertySource<?>> propertySources = new YamlPropertySourceLoader().load(
                        "nacos-integration",
                        new ByteArrayResource(pushedConfig.get().getBytes(StandardCharsets.UTF_8)));
                propertySources.forEach(source -> environment.getPropertySources().addFirst(source));
                Binder.get(environment).bind(
                        "peach.gateway.rate-limit", Bindable.ofInstance(properties));

                context.publishEvent(new EnvironmentChangeEvent(Set.of(
                        "peach.gateway.rate-limit.enabled",
                        "peach.gateway.rate-limit.failure-mode",
                        "peach.gateway.rate-limit.max-matched-rules-per-request",
                        "peach.gateway.rate-limit.allowlist-ips",
                        "peach.gateway.rate-limit.scripts.allowed-remote-hosts",
                        "peach.gateway.rate-limit.scripts.allowed-remote-ports")));

                assertThat(registry.getSnapshot()).isSameAs(previous);

                context.publishEvent(new RefreshScopeRefreshedEvent("nacos-rate-limit-integration"));

                assertThat(registry.getSnapshot()).isNotSameAs(previous);
                assertThat(registry.getSnapshot().enabled()).isTrue();
                assertThat(registry.getSnapshot().failureMode())
                        .isEqualTo(GatewayRateLimitProperties.FailureMode.FAIL_OPEN);
                assertThat(registry.getSnapshot().maxMatchedRulesPerRequest()).isEqualTo(7);
                assertThat(registry.getSnapshot().allowlistIps()).containsExactly("192.0.2.55");
                assertThat(registry.getSnapshot().scripts().allowedRemoteHosts())
                        .containsExactly("cdn.example.com");
                assertThat(registry.getSnapshot().scripts().allowedRemotePorts()).containsExactly(443);
            }
        } finally {
            configService.removeListener(DATA_ID, GROUP, listener);
            configService.shutDown();
        }
    }
}
