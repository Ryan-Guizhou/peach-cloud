package com.peach.gateway.core.ratelimit.script;

import com.peach.gateway.core.ratelimit.config.GatewayRateLimitProperties;
import com.peach.gateway.core.ratelimit.model.RateLimitAlgorithm;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 限流脚本远程来源优先级和校验测试。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/05
 */
class RateLimitScriptRepositoryTest {

    private HttpServer server;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
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
        scripts.setRemoteUrls(Map.of("token-bucket",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/token-bucket.lua"));
        scripts.setExpectedSha256(Map.of("token-bucket", sha256(remoteScript)));

        RateLimitScriptRepository repository = new RateLimitScriptRepository(
                properties, new DefaultResourceLoader());
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
        scripts.setRemoteUrls(Map.of("token-bucket",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/broken.lua"));
        scripts.setExpectedSha256(Map.of("token-bucket", "0".repeat(64)));

        RateLimitScriptRepository repository = new RateLimitScriptRepository(
                properties, new DefaultResourceLoader());

        assertThatThrownBy(() -> repository.reload().block(Duration.ofSeconds(10)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to load remote rate limit script");
    }

    @Test
    void shouldRetainPreviousScriptSnapshotWhenRemoteReloadFails() throws Exception {
        String remoteScript = "return {1, 2, 3, 0, 10}";
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        GatewayRateLimitProperties.ScriptProperties scripts = properties.getScripts();
        scripts.setRemoteUrls(Map.of("token-bucket",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/token-bucket.lua"));
        scripts.setExpectedSha256(Map.of("token-bucket", sha256(remoteScript)));
        RateLimitScriptRepository repository = new RateLimitScriptRepository(
                properties, new DefaultResourceLoader());
        repository.reload().block(Duration.ofSeconds(10));

        scripts.setRemoteUrls(Map.of("token-bucket",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/broken.lua"));

        assertThatThrownBy(() -> repository.reload().block(Duration.ofSeconds(10)))
                .isInstanceOf(RuntimeException.class);
        assertThat(repository.get(RateLimitAlgorithm.TOKEN_BUCKET)).isEqualTo(remoteScript);
    }

    @Test
    void shouldRejectRemoteScriptThatTouchesAnotherRedisKeyOrMetadataAddress() throws Exception {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.getScripts().setRemoteUrls(Map.of("token-bucket", "http://169.254.169.254/latest/meta-data"));

        RateLimitScriptRepository blockedHost = new RateLimitScriptRepository(properties, new DefaultResourceLoader());
        assertThatThrownBy(() -> blockedHost.reload().block(Duration.ofSeconds(5)))
                .isInstanceOf(RuntimeException.class);

        String crossSlotScript = "redis.call('GET', KEYS[2])";
        properties.getScripts().setRemoteUrls(Map.of("token-bucket",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/token-bucket.lua"));
        properties.getScripts().setExpectedSha256(Map.of("token-bucket", sha256(crossSlotScript)));
        server.removeContext("/token-bucket.lua");
        server.createContext("/token-bucket.lua", exchange -> {
            byte[] body = crossSlotScript.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        RateLimitScriptRepository blockedScript = new RateLimitScriptRepository(properties, new DefaultResourceLoader());
        assertThatThrownBy(() -> blockedScript.reload().block(Duration.ofSeconds(5)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to load remote rate limit script");
    }

    private String sha256(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }
}
