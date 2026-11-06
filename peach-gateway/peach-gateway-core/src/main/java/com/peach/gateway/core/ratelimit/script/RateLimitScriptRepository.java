package com.peach.gateway.core.ratelimit.script;

import com.peach.gateway.core.ratelimit.model.RateLimitAlgorithm;
import com.peach.gateway.core.ratelimit.model.RateLimitConfigurationSnapshot;
import com.peach.gateway.core.ratelimit.model.RateLimitScriptConfiguration;
import com.peach.gateway.core.ratelimit.service.RateLimitRuleRegistry;
import io.netty.channel.ChannelOption;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
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
 * <p>每次 reload 固定读取同一份已校验的运行时配置快照。配置远程 OSS URL 时必须使用远程脚本；
 * 只有未配置远程 URL 时才读取本地 classpath。远程下载或校验失败时保留上一份完整脚本快照，
 * 不静默回退到本地版本。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/05
 */
@Component
public class RateLimitScriptRepository {

    private final RateLimitRuleRegistry ruleRegistry;
    private final ResourceLoader resourceLoader;
    private final AtomicReference<ScriptSnapshot> snapshot =
            new AtomicReference<>(ScriptSnapshot.empty());

    private final AtomicLong reloadGeneration = new AtomicLong();

    public RateLimitScriptRepository(RateLimitRuleRegistry ruleRegistry, ResourceLoader resourceLoader) {
        this.ruleRegistry = ruleRegistry;
        this.resourceLoader = resourceLoader;
        validateScriptConfiguration(ruleRegistry.getSnapshot().scripts());
    }

    public Mono<Void> reload() {
        return Mono.defer(() -> {
            RateLimitConfigurationSnapshot runtimeSnapshot = ruleRegistry.getSnapshot();
            RateLimitScriptConfiguration config = runtimeSnapshot.scripts();
            validateScriptConfiguration(config);
            long generation = reloadGeneration.incrementAndGet();
            EnumMap<RateLimitAlgorithm, String> loaded = new EnumMap<>(RateLimitAlgorithm.class);
            return Flux.fromArray(RateLimitAlgorithm.values())
                    .concatMap(algorithm -> load(algorithm, config)
                            .doOnNext(value -> loaded.put(algorithm, value)))
                    .then(Mono.fromRunnable(() -> {
                        if (generation == reloadGeneration.get()
                                && ruleRegistry.getSnapshot().scripts().equals(config)) {
                            snapshot.set(new ScriptSnapshot(Map.copyOf(loaded), config));
                        }
                    }));
        });
    }

    public boolean isReady() {
        ScriptSnapshot active = snapshot.get();
        return active.scripts().size() == RateLimitAlgorithm.values().length
                && active.configuration() != null
                && active.configuration().equals(ruleRegistry.getSnapshot().scripts());
    }

    public String get(RateLimitAlgorithm algorithm) {
        String script = snapshot.get().scripts().get(algorithm);
        if (script == null) {
            throw new IllegalStateException("Rate limit script is not loaded: " + algorithm);
        }
        return script;
    }

    private Mono<String> load(RateLimitAlgorithm algorithm, RateLimitScriptConfiguration config) {
        String remoteUrl = resolveRemoteUrl(algorithm, config);
        if (remoteUrl == null) {
            return loadLocal(algorithm, config);
        }

        RateLimitRemoteScriptAddressValidator.ValidatedTarget target =
                validateRemoteUrl(remoteUrl, algorithm, config);
        String expectedChecksum = expectedChecksum(algorithm, config);
        if (config.requireRemoteChecksum() && (expectedChecksum == null || expectedChecksum.isBlank())) {
            return Mono.error(new IllegalStateException(
                    "Remote rate limit script requires SHA-256 checksum: " + algorithm));
        }

        return remoteWebClient(target, config).get()
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
                .timeout(config.readTimeout())
                .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                .flatMap(value -> validate(algorithm, value, expectedChecksum, config).thenReturn(value))
                .onErrorMap(error -> new IllegalStateException(
                        "Failed to load remote rate limit script: " + algorithm, error));
    }

    /**
     * 为已校验的目标地址创建独立 HTTP 客户端，并将 TCP 连接固定到已校验的 DNS 解析结果，
     * 避免连接阶段再次解析域名而产生 DNS rebinding 窗口。
     */
    private WebClient remoteWebClient(RateLimitRemoteScriptAddressValidator.ValidatedTarget target,
                                      RateLimitScriptConfiguration config) {
        HttpClient httpClient = HttpClient.create()
                .remoteAddress(() -> new InetSocketAddress(target.address(), target.port()))
                .followRedirect(false)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        Math.toIntExact(config.connectTimeout().toMillis()))
                .responseTimeout(config.readTimeout());
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(config.maxScriptBytes()))
                .build();
    }

    private Mono<String> loadLocal(RateLimitAlgorithm algorithm, RateLimitScriptConfiguration config) {
        String location = config.localLocation();
        String path = location.endsWith("/") ? location : location + "/";
        Resource resource = resourceLoader.getResource(path + algorithm.getScriptFileName());
        return Mono.fromCallable(() -> {
                    try (var input = resource.getInputStream()) {
                        byte[] bytes = input.readNBytes(config.maxScriptBytes() + 1);
                        if (bytes.length > config.maxScriptBytes()) {
                            throw new IllegalStateException("Rate limit Lua script exceeds configured size limit");
                        }
                        return new String(bytes, StandardCharsets.UTF_8);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(value -> validate(algorithm, value, expectedChecksum(algorithm, config), config)
                        .thenReturn(value));
    }

    private Mono<Void> validate(RateLimitAlgorithm algorithm,
                                String value,
                                String expectedChecksum,
                                RateLimitScriptConfiguration config) {
        return Mono.fromRunnable(() -> {
            if (value == null || value.isBlank()
                    || value.getBytes(StandardCharsets.UTF_8).length > config.maxScriptBytes()) {
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

    private String expectedChecksum(RateLimitAlgorithm algorithm, RateLimitScriptConfiguration config) {
        Map<String, String> checksums = config.expectedSha256();
        return checksums.getOrDefault(algorithm.name(), checksums.get(algorithm.getConfigKey()));
    }

    private String resolveRemoteUrl(RateLimitAlgorithm algorithm, RateLimitScriptConfiguration config) {
        Map<String, String> urls = config.remoteUrls();
        String configured = urls.getOrDefault(algorithm.name(), urls.get(algorithm.getConfigKey()));
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        String base = config.remoteBaseUrl();
        if (base == null || base.isBlank()) {
            return null;
        }
        return (base.endsWith("/") ? base : base + "/") + algorithm.getScriptFileName();
    }

    private RateLimitRemoteScriptAddressValidator.ValidatedTarget validateRemoteUrl(
            String value, RateLimitAlgorithm algorithm, RateLimitScriptConfiguration config) {
        try {
            return RateLimitRemoteScriptAddressValidator.validate(value, config);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Invalid remote rate limit script URL: " + algorithm, exception);
        }
    }

    private record ScriptSnapshot(
            Map<RateLimitAlgorithm, String> scripts,
            RateLimitScriptConfiguration configuration) {

        private ScriptSnapshot {
            scripts = Map.copyOf(scripts);
        }

        private static ScriptSnapshot empty() {
            return new ScriptSnapshot(Map.of(), null);
        }
    }

    private void validateScriptConfiguration(RateLimitScriptConfiguration config) {
        if (config == null || config.allowedRemotePorts().isEmpty()
                || config.allowedRemotePorts().stream().anyMatch(port -> port == null || port < 1 || port > 65535)) {
            throw new IllegalArgumentException("Invalid gateway rate limit script loading configuration");
        }
    }
}
