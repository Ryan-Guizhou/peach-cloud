package com.peach.gateway.core.ratelimit.script;

import com.peach.gateway.core.ratelimit.config.GatewayRateLimitProperties;
import com.peach.gateway.core.ratelimit.model.RateLimitAlgorithm;
import io.netty.channel.ChannelOption;
import org.springframework.http.HttpHeaders;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 限流 Lua 脚本仓库。
 *
 * <p>为每种算法独立加载脚本。配置远程 OSS URL 时必须使用远程脚本；只有未配置远程 URL 时才读取本地
 * classpath。远程下载或校验失败时保留上一份完整脚本快照并报告失败，不静默回退到本地版本。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/05
 */
@Component
public class RateLimitScriptRepository {

    private final GatewayRateLimitProperties properties;
    private final ResourceLoader resourceLoader;
    private final AtomicReference<Map<RateLimitAlgorithm, String>> scripts =
            new AtomicReference<>(Map.of());

    private final AtomicLong reloadGeneration = new AtomicLong();

    public RateLimitScriptRepository(GatewayRateLimitProperties properties, ResourceLoader resourceLoader) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
        validateScriptConfiguration(properties.getScripts());
    }

    public Mono<Void> reload() {
        return Mono.defer(() -> {
            long generation = reloadGeneration.incrementAndGet();
            validateScriptConfiguration(properties.getScripts());
            EnumMap<RateLimitAlgorithm, String> loaded = new EnumMap<>(RateLimitAlgorithm.class);
            return Flux.fromArray(RateLimitAlgorithm.values())
                    .concatMap(algorithm -> load(algorithm).doOnNext(value -> loaded.put(algorithm, value)))
                    .then(Mono.fromRunnable(() -> {
                        if (generation == reloadGeneration.get()) {
                            scripts.set(Map.copyOf(loaded));
                        }
                    }));
        });
    }

    public boolean isReady() {
        return scripts.get().size() == RateLimitAlgorithm.values().length;
    }

    public String get(RateLimitAlgorithm algorithm) {
        String script = scripts.get().get(algorithm);
        if (script == null) {
            throw new IllegalStateException("Rate limit script is not loaded: " + algorithm);
        }
        return script;
    }

    private Mono<String> load(RateLimitAlgorithm algorithm) {
        String remoteUrl = resolveRemoteUrl(algorithm);
        if (remoteUrl == null) {
            return loadLocal(algorithm);
        }

        RateLimitRemoteScriptAddressValidator.ValidatedTarget target =
                validateRemoteUrl(remoteUrl, algorithm);
        String expectedChecksum = expectedChecksum(algorithm);
        if (properties.getScripts().isRequireRemoteChecksum()
                && (expectedChecksum == null || expectedChecksum.isBlank())) {
            return Mono.error(new IllegalStateException(
                    "Remote rate limit script requires SHA-256 checksum: " + algorithm));
        }

        return remoteWebClient(target).get()
                .uri(target.uri())
                .header(HttpHeaders.HOST, target.uri().getRawAuthority())
                .exchangeToMono(response -> {
                    if (!response.statusCode().is2xxSuccessful()) {
                        return response.releaseBody().then(Mono.error(new IllegalStateException(
                                "Remote rate limit script returned HTTP " + response.statusCode().value())));
                    }
                    return response.bodyToMono(byte[].class)
                            .switchIfEmpty(Mono.error(new IllegalStateException(
                                    "Remote rate limit script response body is empty")));
                })
                .timeout(properties.getScripts().getReadTimeout())
                .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                .flatMap(value -> validate(algorithm, value, expectedChecksum).thenReturn(value))
                .onErrorMap(error -> new IllegalStateException(
                        "Failed to load remote rate limit script: " + algorithm, error));
    }

    /**
     * 为已校验的目标地址创建独立 HTTP 客户端，并将 TCP 连接固定到已校验的 DNS 解析结果，
     * 避免连接阶段再次解析域名而产生 DNS rebinding 窗口。
     */
    private WebClient remoteWebClient(RateLimitRemoteScriptAddressValidator.ValidatedTarget target) {
        GatewayRateLimitProperties.ScriptProperties config = properties.getScripts();
        HttpClient httpClient = HttpClient.create()
                .remoteAddress(() -> new InetSocketAddress(target.address(), target.port()))
                .followRedirect(false)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        Math.toIntExact(config.getConnectTimeout().toMillis()))
                .responseTimeout(config.getReadTimeout());
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(config.getMaxScriptBytes()))
                .build();
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
                .flatMap(value -> validate(algorithm, value, expectedChecksum(algorithm)).thenReturn(value));
    }

    private Mono<Void> validate(RateLimitAlgorithm algorithm, String value, String expectedChecksum) {
        return Mono.fromRunnable(() -> {
            if (value == null || value.isBlank()
                    || value.getBytes(StandardCharsets.UTF_8).length > properties.getScripts().getMaxScriptBytes()) {
                throw new IllegalArgumentException("Invalid rate limit Lua script: " + algorithm);
            }
            RateLimitLuaScriptValidator.validate(value);
            if (expectedChecksum == null || expectedChecksum.isBlank()) {
                return;
            }
            try {
                byte[] digest = MessageDigest.getInstance("SHA-256")
                        .digest(value.getBytes(StandardCharsets.UTF_8));
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

    private RateLimitRemoteScriptAddressValidator.ValidatedTarget validateRemoteUrl(
            String value, RateLimitAlgorithm algorithm) {
        try {
            return RateLimitRemoteScriptAddressValidator.validate(value, properties.getScripts());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Invalid remote rate limit script URL: " + algorithm, exception);
        }
    }

    private void validateScriptConfiguration(GatewayRateLimitProperties.ScriptProperties config) {
        if (config == null || config.getConnectTimeout() == null || config.getConnectTimeout().isZero()
                || config.getConnectTimeout().isNegative()
                || config.getReadTimeout() == null || config.getReadTimeout().isZero()
                || config.getReadTimeout().isNegative()
                || config.getMaxScriptBytes() <= 0 || config.getMaxScriptBytes() > 1_048_576
                || config.getAllowedRemotePorts() == null
                || config.getAllowedRemotePorts().isEmpty()
                || config.getAllowedRemotePorts().stream().anyMatch(port -> port == null || port < 1 || port > 65535)) {
            throw new IllegalArgumentException("Invalid gateway rate limit script loading configuration");
        }
        long connectTimeoutMs = config.getConnectTimeout().toMillis();
        if (connectTimeoutMs <= 0 || connectTimeoutMs > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Rate limit script connect timeout is outside supported range");
        }
    }
}
