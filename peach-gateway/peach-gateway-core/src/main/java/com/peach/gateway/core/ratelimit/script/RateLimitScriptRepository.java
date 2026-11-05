package com.peach.gateway.core.ratelimit.script;

import com.peach.gateway.core.ratelimit.config.GatewayRateLimitProperties;
import com.peach.gateway.core.ratelimit.model.RateLimitAlgorithm;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 限流 Lua 脚本仓库。
 *
 * <p>远程 OSS 地址优先；未配置远程地址时读取本地 classpath。
 * 远程地址配置错误或不可用时，只有显式启用 fallback-to-local 才回退本地资源。</p>
 */
@Component
public class RateLimitScriptRepository {

    private final GatewayRateLimitProperties properties;
    private final ResourceLoader resourceLoader;
    private final WebClient webClient;
    private final AtomicReference<Map<RateLimitAlgorithm, String>> scripts =
            new AtomicReference<>(Map.of());

    public RateLimitScriptRepository(GatewayRateLimitProperties properties, ResourceLoader resourceLoader,
                                     WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
        GatewayRateLimitProperties.ScriptProperties config = properties.getScripts();
        this.webClient = webClientBuilder
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(config.getMaxScriptBytes()))
                .build();
    }

    public Mono<Void> reload() {
        return Mono.defer(() -> {
            EnumMap<RateLimitAlgorithm, String> loaded = new EnumMap<>(RateLimitAlgorithm.class);
            return reactor.core.publisher.Flux.fromArray(RateLimitAlgorithm.values())
                    .concatMap(algorithm -> load(algorithm).doOnNext(script -> loaded.put(algorithm, script)))
                    .then(Mono.fromRunnable(() -> scripts.set(Map.copyOf(loaded))));
        });
    }

    public String get(RateLimitAlgorithm algorithm) {
        String script = scripts.get().get(algorithm);
        if (script == null) {
            throw new IllegalStateException("Rate limit script is not loaded: " + algorithm);
        }
        return script;
    }

    private Mono<String> load(RateLimitAlgorithm algorithm) {
        GatewayRateLimitProperties.ScriptProperties config = properties.getScripts();
        String remoteUrl = resolveRemoteUrl(algorithm);
        if (remoteUrl == null) {
            return loadLocal(algorithm);
        }
        return webClient.get().uri(remoteUrl).retrieve().bodyToMono(byte[].class)
                .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                .flatMap(script -> validate(algorithm, script).thenReturn(script))
                .onErrorResume(error -> {
                    if (!config.isFallbackToLocal()) {
                        return Mono.error(new IllegalStateException(
                                "Failed to load remote rate limit script: " + algorithm, error));
                    }
                    return loadLocal(algorithm);
                });
    }

    private Mono<String> loadLocal(RateLimitAlgorithm algorithm) {
        String location = properties.getScripts().getLocalLocation();
        String path = location.endsWith("/") ? location : location + "/";
        Resource resource = resourceLoader.getResource(path + algorithm.getScriptFileName());
        return Mono.fromCallable(() -> {
            try (var input = resource.getInputStream()) {
                byte[] bytes = input.readNBytes(properties.getScripts().getMaxScriptBytes() + 1);
                if (bytes.length > properties.getScripts().getMaxScriptBytes()) {
                    throw new IllegalStateException("Rate limit Lua script exceeds configured size limit");
                }
                String script = new String(bytes, StandardCharsets.UTF_8);
                validate(algorithm, script).block(Duration.ofSeconds(2));
                return script;
            }
        });
    }

    private Mono<Void> validate(RateLimitAlgorithm algorithm, String script) {
        if (script == null || script.isBlank()
                || script.length() > properties.getScripts().getMaxScriptBytes()) {
            return Mono.error(new IllegalArgumentException("Invalid rate limit Lua script: " + algorithm));
        }
        String expected = expectedChecksum(algorithm);
        if (expected == null || expected.isBlank()) {
            return Mono.empty();
        }
        return Mono.fromRunnable(() -> {
            try {
                byte[] digest = MessageDigest.getInstance("SHA-256")
                        .digest(script.getBytes(StandardCharsets.UTF_8));
                String actual = java.util.HexFormat.of().formatHex(digest);
                if (!actual.equalsIgnoreCase(expected.trim())) {
                    throw new IllegalArgumentException("Rate limit Lua SHA-256 mismatch: " + algorithm);
                }
            } catch (java.security.NoSuchAlgorithmException exception) {
                throw new IllegalStateException("SHA-256 is unavailable", exception);
            }
        });
    }

    private String expectedChecksum(RateLimitAlgorithm algorithm) {
        Map<String, String> checksums = properties.getScripts().getExpectedSha256();
        return checksums.getOrDefault(algorithm.name(),
                checksums.get(algorithm.getConfigKey()));
    }

    private String resolveRemoteUrl(RateLimitAlgorithm algorithm) {
        Map<String, String> urls = properties.getScripts().getRemoteUrls();
        String configured = urls.getOrDefault(algorithm.name(),
                urls.get(algorithm.getConfigKey()));
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        String base = properties.getScripts().getRemoteBaseUrl();
        if (base == null || base.isBlank()) {
            return null;
        }
        return (base.endsWith("/") ? base : base + "/") + algorithm.getScriptFileName();
    }
}
