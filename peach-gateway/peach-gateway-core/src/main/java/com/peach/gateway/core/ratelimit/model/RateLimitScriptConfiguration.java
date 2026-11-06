package com.peach.gateway.core.ratelimit.model;

import com.peach.gateway.core.ratelimit.config.GatewayRateLimitProperties;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 限流 Lua 脚本加载配置的不可变快照。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/06
 */
public record RateLimitScriptConfiguration(
        String remoteBaseUrl,
        Map<String, String> remoteUrls,
        String localLocation,
        boolean requireRemoteChecksum,
        boolean requireHttps,
        Duration connectTimeout,
        Duration readTimeout,
        int maxScriptBytes,
        Map<String, String> expectedSha256,
        Set<String> allowedRemoteHosts,
        List<String> allowedRemoteCidrs,
        Set<Integer> allowedRemotePorts) {

    public RateLimitScriptConfiguration {
        remoteUrls = remoteUrls == null ? Map.of() : Map.copyOf(remoteUrls);
        expectedSha256 = expectedSha256 == null ? Map.of() : Map.copyOf(expectedSha256);
        allowedRemoteHosts = allowedRemoteHosts == null ? Set.of() : Set.copyOf(allowedRemoteHosts);
        allowedRemoteCidrs = allowedRemoteCidrs == null ? List.of() : List.copyOf(allowedRemoteCidrs);
        allowedRemotePorts = allowedRemotePorts == null ? Set.of(443) : Set.copyOf(allowedRemotePorts);
        if (allowedRemotePorts.isEmpty()
                || allowedRemotePorts.stream().anyMatch(port -> port == null || port < 1 || port > 65535)) {
            throw new IllegalArgumentException("Remote rate limit script ports must be within 1..65535");
        }
        if (localLocation == null || localLocation.isBlank()
                || connectTimeout == null || connectTimeout.isZero() || connectTimeout.isNegative()
                || readTimeout == null || readTimeout.isZero() || readTimeout.isNegative()
                || maxScriptBytes < 1 || maxScriptBytes > 1_048_576) {
            throw new IllegalArgumentException("Invalid gateway rate limit script configuration");
        }
        long connectTimeoutMillis = connectTimeout.toMillis();
        if (connectTimeoutMillis < 1 || connectTimeoutMillis > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Rate limit script connect timeout is outside supported range");
        }
    }

    public static RateLimitScriptConfiguration from(GatewayRateLimitProperties.ScriptProperties properties) {
        if (properties == null) {
            throw new IllegalArgumentException("Gateway rate limit script configuration is required");
        }
        return new RateLimitScriptConfiguration(
                properties.getRemoteBaseUrl(),
                properties.getRemoteUrls(),
                properties.getLocalLocation(),
                properties.isRequireRemoteChecksum(),
                properties.isRequireHttps(),
                properties.getConnectTimeout(),
                properties.getReadTimeout(),
                properties.getMaxScriptBytes(),
                properties.getExpectedSha256(),
                properties.getAllowedRemoteHosts() == null
                        ? Set.of() : Set.copyOf(properties.getAllowedRemoteHosts()),
                properties.getAllowedRemoteCidrs(),
                properties.getAllowedRemotePorts() == null
                        ? Set.of(443) : Set.copyOf(properties.getAllowedRemotePorts()));
    }

    public static RateLimitScriptConfiguration empty() {
        return new RateLimitScriptConfiguration(
                null,
                Map.of(),
                "classpath:/ratelimit/lua/",
                true,
                true,
                Duration.ofSeconds(2),
                Duration.ofSeconds(5),
                65536,
                Map.of(),
                Set.of(),
                List.of(),
                Set.of(443));
    }
}
