package com.peach.gateway.core.ratelimit.script;

import com.peach.gateway.core.ratelimit.config.GatewayRateLimitProperties;
import com.peach.gateway.core.ratelimit.model.RateLimitAlgorithm;
import io.netty.channel.ChannelOption;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 限流 Lua 脚本仓库。
 *
 * <p>远程 OSS 地址优先；未配置远程地址时读取本地 classpath。
 * 远程地址配置错误或不可用时，只有显式启用 fallback-to-local 才回退本地资源。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/05
 */
@Component
public class RateLimitScriptRepository {

    private final GatewayRateLimitProperties properties;
    private final ResourceLoader resourceLoader;
    private final WebClient webClient;
    private final AtomicReference<Map<RateLimitAlgorithm, String>> scripts =
            new AtomicReference<>(Map.of());

    public RateLimitScriptRepository(GatewayRateLimitProperties properties, ResourceLoader resourceLoader) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
        GatewayRateLimitProperties.ScriptProperties config = properties.getScripts();
        validateScriptConfiguration(config);
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(config.getConnectTimeout().toMillis()))
                .responseTimeout(config.getReadTimeout());
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(config.getMaxScriptBytes()))
                .build();
    }

    public Mono<Void> reload() {
        return Mono.defer(() -> {
            EnumMap<RateLimitAlgorithm, String> loaded = new EnumMap<>(RateLimitAlgorithm.class);
            return Flux.fromArray(RateLimitAlgorithm.values())
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

        String expectedChecksum = expectedChecksum(algorithm);
        if (config.isRequireRemoteChecksum() && (expectedChecksum == null || expectedChecksum.isBlank())) {
            return fallbackOrError(algorithm,
                    new IllegalStateException("Remote rate limit script requires SHA-256: " + algorithm));
        }

        return webClient.get().uri(remoteUrl).retrieve().bodyToMono(byte[].class)
                .timeout(config.getReadTimeout())
                .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                .flatMap(script -> validate(algorithm, script, expectedChecksum).thenReturn(script))
                .onErrorResume(error -> fallbackOrError(algorithm, error));
    }

    private Mono<String> fallbackOrError(RateLimitAlgorithm algorithm, Throwable error) {
        if (!properties.getScripts().isFallbackToLocal()) {
            return Mono.error(new IllegalStateException(
                    "Failed to load remote rate limit script: " + algorithm, error));
        }
        return loadLocal(algorithm);
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
                        return new String(bytes, StandardCharsets.UTF_8);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(script -> validate(algorithm, script, expectedChecksum(algorithm)).thenReturn(script));
    }

    private Mono<Void> validate(RateLimitAlgorithm algorithm, String script, String expectedChecksum) {
        if (script == null || script.isBlank()
                || script.getBytes(StandardCharsets.UTF_8).length > properties.getScripts().getMaxScriptBytes()) {
            return Mono.error(new IllegalArgumentException("Invalid rate limit Lua script: " + algorithm));
        }
        if (expectedChecksum == null || expectedChecksum.isBlank()) {
            return Mono.empty();
        }
        return Mono.fromRunnable(() -> {
            try {
                byte[] digest = MessageDigest.getInstance("SHA-256")
                        .digest(script.getBytes(StandardCharsets.UTF_8));
                String actual = java.util.HexFormat.of().formatHex(digest);
                if (!actual.equalsIgnoreCase(expectedChecksum.trim())) {
                    throw new IllegalArgumentException("Rate limit Lua SHA-256 mismatch: " + algorithm);
                }
            } catch (java.security.NoSuchAlgorithmException exception) {
                throw new IllegalStateException("SHA-256 is unavailable", exception);
            }
        });
    }

    private String expectedChecksum(RateLimitAlgorithm algorithm) {
        Map<String, String> checksums = properties.getScripts().getExpectedSha256();
        if (checksums == null) {
            return null;
        }
        return checksums.getOrDefault(algorithm.name(), checksums.get(algorithm.getConfigKey()));
    }

    private String resolveRemoteUrl(RateLimitAlgorithm algorithm) {
        Map<String, String> urls = properties.getScripts().getRemoteUrls();
        if (urls != null) {
            String configured = urls.getOrDefault(algorithm.name(), urls.get(algorithm.getConfigKey()));
            if (configured != null && !configured.isBlank()) {
                return configured.trim();
            }
        }
        String base = properties.getScripts().getRemoteBaseUrl();
        if (base == null || base.isBlank()) {
            return null;
        }
        return (base.endsWith("/") ? base : base + "/") + algorithm.getScriptFileName();
    }

    private void validateScriptConfiguration(GatewayRateLimitProperties.ScriptProperties config) {
        if (config.getConnectTimeout() == null || config.getConnectTimeout().isZero()
                || config.getConnectTimeout().isNegative()
                || config.getReadTimeout() == null || config.getReadTimeout().isZero()
                || config.getReadTimeout().isNegative()
                || config.getMaxScriptBytes() <= 0 || config.getMaxScriptBytes() > 1_048_576) {
            throw new IllegalArgumentException("Invalid gateway rate limit script loading configuration");
        }
        long connectTimeoutMs = config.getConnectTimeout().toMillis();
        if (connectTimeoutMs > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Rate limit script connect timeout is too large");
        }
    }
}
