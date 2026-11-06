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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

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
    private static final NacosContainer NACOS = new NacosContainer();

    private static final class NacosContainer extends GenericContainer<NacosContainer> {

        private NacosContainer() {
            super("nacos/nacos-server:v2.5.2");
            addFixedExposedPort(18848, 8848);
            addFixedExposedPort(19848, 9848);
            addFixedExposedPort(19849, 9849);
            withExposedPorts(8848, 9848, 9849);
            withEnv("MODE", "standalone");
            withEnv("NACOS_AUTH_ENABLE", "false");
            withEnv("JVM_XMS", "256m");
            withEnv("JVM_XMX", "512m");
            waitingFor(Wait.forHttp("/nacos/v1/console/health/readiness")
                    .forPort(8848)
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(3)));
        }
    }

    private String awaitConfig(BlockingQueue<String> pushedConfigs, String marker) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (true) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                return null;
            }
            String config = pushedConfigs.poll(remaining, TimeUnit.NANOSECONDS);
            if (config == null) {
                return null;
            }
            if (config.contains(marker)) {
                return config;
            }
        }
    }

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

        ConfigService publisher = NacosFactory.createConfigService(clientProperties);
        ConfigService listenerClient = NacosFactory.createConfigService(clientProperties);
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
        publishWithRetry(publisher, initialConfig);

        BlockingQueue<String> pushedConfigs = new LinkedBlockingQueue<>();
        Listener listener = new Listener() {
            @Override
            public java.util.concurrent.Executor getExecutor() {
                return null;
            }

            @Override
            public void receiveConfigInfo(String configInfo) {
                if (configInfo != null) {
                    pushedConfigs.offer(configInfo);
                }
            }
        };

        try {
            String initialReceivedConfig = listenerClient.getConfigAndSignListener(
                    DATA_ID, GROUP, 5000, listener);
            assertThat(initialReceivedConfig).contains("enabled: false");
            publishWithRetry(publisher, updatedConfig);
            String receivedConfig = awaitConfig(pushedConfigs, "192.0.2.55");
            assertThat(receivedConfig)
                    .as("Nacos listener should receive the updated configuration")
                    .isNotNull();

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
                        new ByteArrayResource(receivedConfig.getBytes(StandardCharsets.UTF_8)));
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

                var lastKnownGood = registry.getSnapshot();
                String invalidConfig = updatedConfig
                        .replace("max-matched-rules-per-request: 7", "max-matched-rules-per-request: 0")
                        .replace("192.0.2.55", "192.0.2.56");
                publishWithRetry(publisher, invalidConfig);
                String receivedInvalidConfig = awaitConfig(pushedConfigs, "192.0.2.56");
                assertThat(receivedInvalidConfig).isNotNull();

                List<PropertySource<?>> invalidSources = new YamlPropertySourceLoader().load(
                        "nacos-invalid-integration",
                        new ByteArrayResource(receivedInvalidConfig.getBytes(StandardCharsets.UTF_8)));
                invalidSources.forEach(source -> environment.getPropertySources().addFirst(source));
                Binder.get(environment).bind("peach.gateway.rate-limit", Bindable.ofInstance(properties));

                context.publishEvent(new EnvironmentChangeEvent(Set.of(
                        "peach.gateway.rate-limit.max-matched-rules-per-request",
                        "peach.gateway.rate-limit.allowlist-ips")));
                assertThat(registry.getSnapshot()).isSameAs(lastKnownGood);

                context.publishEvent(new RefreshScopeRefreshedEvent("nacos-invalid-rate-limit-integration"));
                assertThat(registry.getSnapshot()).isSameAs(lastKnownGood);
                assertThat(registry.getSnapshot().maxMatchedRulesPerRequest()).isEqualTo(7);
                assertThat(registry.getSnapshot().allowlistIps()).containsExactly("192.0.2.55");
            }
        } finally {
            listenerClient.removeListener(DATA_ID, GROUP, listener);
            listenerClient.shutDown();
            publisher.shutDown();
        }
    }
}
