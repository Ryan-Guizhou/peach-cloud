package com.peach.gateway.core.ratelimit.service;

import com.peach.gateway.core.ratelimit.config.GatewayRateLimitProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * 按可信代理 CIDR 解析真实客户端 IP。
 *
 * <p>只有 TCP 对端属于可信代理时才解析 X-Forwarded-For，并从右向左剥离可信代理跳点。
 * 非可信对端提供的转发头一律忽略，防止客户端伪造限流身份。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/05
 */
@Slf4j
@Component
public class TrustedProxyClientIpResolver {

    private static final int MAX_FORWARDED_HOPS = 32;

    private static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";

    private static final Pattern IP_LITERAL_PATTERN = Pattern.compile("[0-9a-fA-F:.]+");

    private final GatewayRateLimitProperties properties;

    private final AtomicReference<List<IpNetwork>> trustedNetworks = new AtomicReference<>(List.of());

    public TrustedProxyClientIpResolver(GatewayRateLimitProperties properties) {
        this.properties = properties;
        trustedNetworks.set(parseNetworks(properties.getTrustedProxies()));
    }

    /**
     * 解析客户端地址。无法解析时返回 {@code unknown}。
     *
     * @param exchange 当前请求上下文
     * @return 客户端 IP 字面量
     */
    public String resolve(ServerWebExchange exchange) {
        InetAddress remoteAddress = remoteAddress(exchange);
        if (remoteAddress == null) {
            return "unknown";
        }
        if (!isTrusted(remoteAddress)) {
            return remoteAddress.getHostAddress();
        }

        List<String> forwardedHeaders = exchange.getRequest().getHeaders().get(X_FORWARDED_FOR_HEADER);
        if (forwardedHeaders == null || forwardedHeaders.isEmpty()) {
            return remoteAddress.getHostAddress();
        }
        String forwardedFor = String.join(",", forwardedHeaders);
        if (forwardedFor.isBlank()) {
            return remoteAddress.getHostAddress();
        }

        String[] values = forwardedFor.split(",", -1);
        if (values.length == 0 || values.length > MAX_FORWARDED_HOPS) {
            return remoteAddress.getHostAddress();
        }

        List<InetAddress> chain = new ArrayList<>(values.length);
        for (String value : values) {
            InetAddress hop = parseAddress(value);
            if (hop == null) {
                return remoteAddress.getHostAddress();
            }
            chain.add(hop);
        }

        for (int index = chain.size() - 1; index >= 0; index--) {
            InetAddress hop = chain.get(index);
            if (!isTrusted(hop)) {
                return hop.getHostAddress();
            }
        }
        return remoteAddress.getHostAddress();
    }

    @Order(Ordered.LOWEST_PRECEDENCE)
    @EventListener(EnvironmentChangeEvent.class)
    public void onEnvironmentChanged(EnvironmentChangeEvent event) {
        try {
            trustedNetworks.set(parseNetworks(properties.getTrustedProxies()));
            log.info("Gateway trusted proxy configuration refreshed, cidrCount={}", trustedNetworks.get().size());
        } catch (IllegalArgumentException exception) {
            log.error("Gateway trusted proxy configuration refresh rejected, reason={}",
                    exception.getClass().getSimpleName());
        }
    }

    private InetAddress remoteAddress(ServerWebExchange exchange) {
        InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
        if (remote == null) {
            return null;
        }
        if (remote.getAddress() != null) {
            return remote.getAddress();
        }
        return parseAddress(remote.getHostString());
    }

    private boolean isTrusted(InetAddress address) {
        return trustedNetworks.get().stream().anyMatch(network -> network.contains(address));
    }

    private List<IpNetwork> parseNetworks(List<String> configured) {
        if (configured == null || configured.isEmpty()) {
            return List.of();
        }
        return configured.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(IpNetwork::parse)
                .toList();
    }

    /**
     * 将 IP 字面量规范化为与 InetAddress.getHostAddress 一致的表示。
     *
     * @param value IP 字面量
     * @return 规范化地址；非法地址返回 null
     */
    static String canonicalizeAddress(String value) {
        InetAddress address = parseAddress(value);
        return address == null ? null : address.getHostAddress();
    }

    private static InetAddress parseAddress(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty() || !IP_LITERAL_PATTERN.matcher(normalized).matches()) {
            return null;
        }
        try {
            return InetAddress.getByName(normalized);
        } catch (UnknownHostException exception) {
            return null;
        }
    }

    private record IpNetwork(byte[] address, int prefixLength) {

        private static IpNetwork parse(String value) {
            String[] parts = value.trim().split("/", -1);
            if (parts.length > 2) {
                throw new IllegalArgumentException("Invalid trusted proxy CIDR");
            }
            InetAddress address = parseAddress(parts[0]);
            if (address == null) {
                throw new IllegalArgumentException("Invalid trusted proxy IP address");
            }
            int maxPrefix = address.getAddress().length * Byte.SIZE;
            int prefix = maxPrefix;
            if (parts.length == 2) {
                try {
                    prefix = Integer.parseInt(parts[1]);
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("Invalid trusted proxy CIDR prefix", exception);
                }
            }
            if (prefix < 0 || prefix > maxPrefix) {
                throw new IllegalArgumentException("Trusted proxy CIDR prefix is outside supported range");
            }
            return new IpNetwork(address.getAddress(), prefix);
        }

        private boolean contains(InetAddress candidate) {
            byte[] candidateBytes = candidate.getAddress();
            if (candidateBytes.length != address.length) {
                return false;
            }
            int fullBytes = prefixLength / Byte.SIZE;
            int remainingBits = prefixLength % Byte.SIZE;
            for (int index = 0; index < fullBytes; index++) {
                if (candidateBytes[index] != address[index]) {
                    return false;
                }
            }
            if (remainingBits == 0) {
                return true;
            }
            int mask = 0xFF << (Byte.SIZE - remainingBits);
            return (candidateBytes[fullBytes] & mask) == (address[fullBytes] & mask);
        }
    }
}
