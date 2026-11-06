package com.peach.gateway.core.ratelimit.script;

import com.peach.gateway.core.ratelimit.config.GatewayRateLimitProperties;
import com.peach.gateway.core.ratelimit.model.RateLimitAlgorithm;
import com.peach.gateway.core.ratelimit.service.RateLimitRuleRegistry;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 限流脚本远程来源优先级和校验测试。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/05
 */
class RateLimitScriptRepositoryTest {

    private HttpServer server;
    private String serverHost;
    private final AtomicBoolean redirectTargetRequested = new AtomicBoolean();

    @BeforeEach
    void startServer() throws Exception {
        serverHost = NetworkInterface.networkInterfaces()
                .flatMap(NetworkInterface::inetAddresses)
                .filter(address -> address instanceof Inet4Address
                        && !address.isLoopbackAddress()
                        && !address.isLinkLocalAddress()
                        && !address.isMulticastAddress())
                .map(InetAddress::getHostAddress)
                .findFirst()
                .orElse(null);
        assumeTrue(serverHost != null, "A non-loopback IPv4 interface is required for remote script tests");
        server = HttpServer.create(new InetSocketAddress("0.0.0.0", 0), 0);
        server.createContext("/token-bucket.lua", exchange -> {
            byte[] body = "return {1, 2, 3, 0, 10}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.createContext("/broken.lua", exchange -> {
            byte[] body = "not found".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(404, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.createContext("/redirect.lua", exchange -> {
            exchange.getResponseHeaders().set("Location", "http://169.254.169.254/latest/meta-data");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/redirect-target.lua", exchange -> {
            redirectTargetRequested.set(true);
            byte[] body = "return {1, 2, 3, 0, 10}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldPreferConfiguredRemoteScriptAndKeepLocalScriptsForUnconfiguredAlgorithms() throws Exception {
        String remoteScript = "return {1, 2, 3, 0, 10}";
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        GatewayRateLimitProperties.ScriptProperties scripts = properties.getScripts();
        allowLocalTestServer(scripts);
        scripts.setRemoteUrls(Map.of("token-bucket",
                localUrl("/token-bucket.lua")));
        scripts.setExpectedSha256(Map.of("token-bucket", sha256(remoteScript)));

        RateLimitScriptRepository repository = repository(properties);
        assertThat(repository.isReady()).isFalse();
        repository.reload().block(Duration.ofSeconds(10));

        assertThat(repository.isReady()).isTrue();
        assertThat(repository.get(RateLimitAlgorithm.TOKEN_BUCKET)).isEqualTo(remoteScript);
        assertThat(repository.get(RateLimitAlgorithm.FIXED_WINDOW))
                .contains("redis.call")
                .doesNotContain("return {1, 2, 3, 0, 10}");
    }

    @Test
    void shouldNotFallbackToLocalWhenConfiguredRemoteUrlFails() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        GatewayRateLimitProperties.ScriptProperties scripts = properties.getScripts();
        allowLocalTestServer(scripts);
        scripts.setRemoteUrls(Map.of("token-bucket", localUrl("/broken.lua")));
        scripts.setExpectedSha256(Map.of("token-bucket", "0".repeat(64)));

        RateLimitScriptRepository repository = repository(properties);

        assertThatThrownBy(() -> repository.reload().block(Duration.ofSeconds(10)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to load remote rate limit script");
    }

    @Test
    void shouldRetainPreviousScriptSnapshotWhenRemoteReloadFails() throws Exception {
        String remoteScript = "return {1, 2, 3, 0, 10}";
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        GatewayRateLimitProperties.ScriptProperties scripts = properties.getScripts();
        allowLocalTestServer(scripts);
        scripts.setRemoteUrls(Map.of("token-bucket", localUrl("/token-bucket.lua")));
        scripts.setExpectedSha256(Map.of("token-bucket", sha256(remoteScript)));
        RateLimitRuleRegistry registry = new RateLimitRuleRegistry(properties);
        RateLimitScriptRepository repository = new RateLimitScriptRepository(registry, new DefaultResourceLoader());
        repository.reload().block(Duration.ofSeconds(10));

        scripts.setRemoteUrls(Map.of("token-bucket", localUrl("/broken.lua")));
        registry.refresh();

        assertThatThrownBy(() -> repository.reload().block(Duration.ofSeconds(10)))
                .isInstanceOf(RuntimeException.class);
        assertThat(repository.get(RateLimitAlgorithm.TOKEN_BUCKET)).isEqualTo(remoteScript);
    }

    @Test
    void shouldRejectMetadataAddressEvenWhenHostIsExplicitlyAllowlisted() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        GatewayRateLimitProperties.ScriptProperties scripts = properties.getScripts();
        scripts.setAllowedRemoteHosts(List.of("169.254.169.254"));
        scripts.setAllowedRemotePorts(List.of(443));
        scripts.setRemoteUrls(Map.of("token-bucket", "https://169.254.169.254/latest/meta-data"));

        RateLimitScriptRepository repository = repository(properties);

        assertThatThrownBy(() -> repository.reload().block(Duration.ofSeconds(5)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldRejectLoopbackEvenWhenHostIsAllowlisted() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        GatewayRateLimitProperties.ScriptProperties scripts = properties.getScripts();
        scripts.setRequireHttps(false);
        scripts.setAllowedRemoteHosts(List.of("127.0.0.1"));
        scripts.setAllowedRemotePorts(List.of(server.getAddress().getPort()));
        scripts.setRemoteUrls(Map.of("token-bucket", localUrl("/token-bucket.lua")));

        RateLimitScriptRepository repository = repository(properties);

        assertThatThrownBy(() -> repository.reload().block(Duration.ofSeconds(5)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldRejectPrivateAddressWithoutExplicitCidrAuthorization() throws Exception {
        assumeTrue(InetAddress.getByName(serverHost).isSiteLocalAddress(),
                "A private test interface is required for this CIDR policy test");
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        GatewayRateLimitProperties.ScriptProperties scripts = properties.getScripts();
        scripts.setRequireHttps(false);
        scripts.setAllowedRemoteHosts(List.of(serverHost));
        scripts.setAllowedRemotePorts(List.of(server.getAddress().getPort()));
        scripts.setRemoteUrls(Map.of("token-bucket", localUrl("/token-bucket.lua")));

        RateLimitScriptRepository repository = repository(properties);

        assertThatThrownBy(() -> repository.reload().block(Duration.ofSeconds(5)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldRejectBroadOrPublicRemoteAuthorizationCidrs() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        GatewayRateLimitProperties.ScriptProperties scripts = properties.getScripts();
        scripts.setRequireHttps(false);
        scripts.setAllowedRemoteHosts(List.of(serverHost));
        scripts.setAllowedRemoteCidrs(List.of("0.0.0.0/0"));
        scripts.setAllowedRemotePorts(List.of(server.getAddress().getPort()));
        scripts.setRemoteUrls(Map.of("token-bucket", localUrl("/token-bucket.lua")));

        RateLimitScriptRepository repository = repository(properties);

        assertThatThrownBy(() -> repository.reload().block(Duration.ofSeconds(5)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldRejectDnsHostResolvingToLoopbackWithoutExplicitCidrAuthorization() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        GatewayRateLimitProperties.ScriptProperties scripts = properties.getScripts();
        scripts.setRequireHttps(false);
        scripts.setAllowedRemoteHosts(List.of("localhost"));
        scripts.setAllowedRemotePorts(List.of(server.getAddress().getPort()));
        scripts.setRemoteUrls(Map.of("token-bucket",
                "http://localhost:" + server.getAddress().getPort() + "/token-bucket.lua"));

        RateLimitScriptRepository repository = repository(properties);

        assertThatThrownBy(() -> repository.reload().block(Duration.ofSeconds(5)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldRejectBracketedIpv6LoopbackWithoutExplicitCidrAuthorization() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        GatewayRateLimitProperties.ScriptProperties scripts = properties.getScripts();
        scripts.setAllowedRemoteHosts(List.of("::1"));
        scripts.setAllowedRemotePorts(List.of(443));
        scripts.setRemoteUrls(Map.of("token-bucket", "https://[::1]/token-bucket.lua"));

        RateLimitScriptRepository repository = repository(properties);

        assertThatThrownBy(() -> repository.reload().block(Duration.ofSeconds(5)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldRejectHostOutsideExplicitAllowlistBeforeConnecting() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        GatewayRateLimitProperties.ScriptProperties scripts = properties.getScripts();
        scripts.setAllowedRemoteHosts(List.of("cdn.example.com"));
        scripts.setAllowedRemotePorts(List.of(443));
        scripts.setRemoteUrls(Map.of("token-bucket", "https://attacker.example/token-bucket.lua"));

        RateLimitScriptRepository repository = repository(properties);

        assertThatThrownBy(() -> repository.reload().block(Duration.ofSeconds(5)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldRejectHttpWhenHttpsIsRequired() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        GatewayRateLimitProperties.ScriptProperties scripts = properties.getScripts();
        scripts.setAllowedRemoteHosts(List.of(serverHost));
        scripts.setAllowedRemoteCidrs(List.of(serverHost + "/32"));
        scripts.setAllowedRemotePorts(List.of(server.getAddress().getPort()));
        scripts.setRemoteUrls(Map.of("token-bucket", localUrl("/token-bucket.lua")));

        RateLimitScriptRepository repository = repository(properties);

        assertThatThrownBy(() -> repository.reload().block(Duration.ofSeconds(5)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldRejectRedirectResponseWithoutFollowingItsLocation() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        GatewayRateLimitProperties.ScriptProperties scripts = properties.getScripts();
        allowLocalTestServer(scripts);
        scripts.setRemoteUrls(Map.of("token-bucket", localUrl("/redirect.lua")));

        RateLimitScriptRepository repository = repository(properties);

        assertThatThrownBy(() -> repository.reload().block(Duration.ofSeconds(5)))
                .isInstanceOf(RuntimeException.class);
        assertThat(redirectTargetRequested).isFalse();
    }

    private RateLimitScriptRepository repository(GatewayRateLimitProperties properties) {
        return new RateLimitScriptRepository(new RateLimitRuleRegistry(properties), new DefaultResourceLoader());
    }

    private void allowLocalTestServer(GatewayRateLimitProperties.ScriptProperties scripts) {
        scripts.setRequireHttps(false);
        scripts.setAllowedRemoteHosts(List.of(serverHost));
        scripts.setAllowedRemoteCidrs(List.of(serverHost + "/32"));
        scripts.setAllowedRemotePorts(List.of(server.getAddress().getPort()));
    }

    private String localUrl(String path) {
        String uriHost = serverHost.contains(":") ? "[" + serverHost + "]" : serverHost;
        return "http://" + uriHost + ":" + server.getAddress().getPort() + path;
    }

    private String sha256(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }
}
