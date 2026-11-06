package com.peach.gateway.core.ratelimit.script;

import com.peach.gateway.core.ratelimit.model.RateLimitAlgorithm;
import com.peach.gateway.core.ratelimit.model.RateLimitScriptConfiguration;

import java.net.IDN;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;

/**
 * 远程 Lua 脚本目标地址校验器。
 *
 * <p>要求精确主机白名单、显式端口白名单，并检查 DNS 解析得到的全部地址。私有网络目标只有在
 * allowed-remote-cidrs 中显式授权后才允许访问；云元数据地址始终拒绝。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/06
 */
public final class RateLimitRemoteScriptAddressValidator {

    private static final List<String> BLOCKED_METADATA_HOSTS = List.of(
            "metadata",
            "metadata.google.internal",
            "metadata.azure.com",
            "instance-data.ec2.internal");

    private RateLimitRemoteScriptAddressValidator() {
    }

    public static void validateConfiguration(RateLimitScriptConfiguration config) {
        if (config == null || config.allowedRemotePorts().isEmpty()
                || config.allowedRemotePorts().stream().anyMatch(port -> port == null || port < 1 || port > 65535)) {
            throw invalid("remote script port allowlist is invalid");
        }
        config.allowedRemoteHosts().forEach(RateLimitRemoteScriptAddressValidator::normalizeHost);
        config.allowedRemoteCidrs().forEach(RateLimitRemoteScriptAddressValidator::parseCidr);
        boolean baseUrlConfigured = config.remoteBaseUrl() != null && !config.remoteBaseUrl().isBlank();
        boolean remoteConfigured = baseUrlConfigured
                || config.remoteUrls().values().stream().anyMatch(value -> value != null && !value.isBlank());
        if (remoteConfigured && config.allowedRemoteHosts().isEmpty()) {
            throw invalid("remote scripts require an exact allowed-remote-hosts entry");
        }
        if (baseUrlConfigured) {
            String baseUrl = config.remoteBaseUrl().trim();
            URI baseUri;
            try {
                baseUri = URI.create(baseUrl);
            } catch (IllegalArgumentException exception) {
                throw invalid("remote script base URL is malformed", exception);
            }
            if (baseUri.getRawQuery() != null || baseUri.getRawFragment() != null) {
                throw invalid("remote script base URL must not contain query or fragment components");
            }
            String probeUrl = (baseUrl.endsWith("/") ? baseUrl : baseUrl + "/")
                    + "__rate_limit_script_validation__.lua";
            validate(probeUrl, config);
        }
        config.remoteUrls().values().stream()
                .filter(value -> value != null && !value.isBlank())
                .forEach(value -> validate(value.trim(), config));
        validateRemoteChecksums(config);
    }

    private static void validateRemoteChecksums(RateLimitScriptConfiguration config) {
        config.expectedSha256().forEach((algorithm, checksum) -> {
            if (checksum == null || !checksum.matches("[0-9a-fA-F]{64}")) {
                throw invalid("SHA-256 checksum must contain exactly 64 hexadecimal characters: " + algorithm);
            }
        });
        if (!config.requireRemoteChecksum()) {
            return;
        }
        for (RateLimitAlgorithm algorithm : RateLimitAlgorithm.values()) {
            String configuredUrl = config.remoteUrls().getOrDefault(
                    algorithm.name(), config.remoteUrls().get(algorithm.getConfigKey()));
            boolean remoteScript = configuredUrl != null && !configuredUrl.isBlank()
                    || config.remoteBaseUrl() != null && !config.remoteBaseUrl().isBlank();
            if (!remoteScript) {
                continue;
            }
            String checksum = config.expectedSha256().getOrDefault(
                    algorithm.name(), config.expectedSha256().get(algorithm.getConfigKey()));
            if (checksum == null || checksum.isBlank()) {
                throw invalid("remote Lua script requires a SHA-256 checksum: " + algorithm.getConfigKey());
            }
        }
    }

    static ValidatedTarget validate(String value, RateLimitScriptConfiguration config) {
        if (value == null || value.isBlank()) {
            throw invalid("remote URL is blank");
        }
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw invalid("remote URL is malformed", exception);
        }

        String scheme = uri.getScheme();
        boolean https = "https".equalsIgnoreCase(scheme);
        boolean http = "http".equalsIgnoreCase(scheme);
        if ((!https && !http) || (config.requireHttps() && !https)
                || uri.getHost() == null || uri.getHost().isBlank()
                || uri.getUserInfo() != null || uri.getFragment() != null) {
            throw invalid("remote URL must be an approved absolute HTTP(S) URL without user-info or fragment");
        }

        String host = normalizeHost(uri.getHost());
        if (isMetadataHost(host)) {
            throw invalid("cloud metadata hosts are never allowed");
        }
        if (!isHostAllowed(host, List.copyOf(config.allowedRemoteHosts()))) {
            throw invalid("remote host is not present in allowed-remote-hosts");
        }

        int port = uri.getPort() < 0 ? (https ? 443 : 80) : uri.getPort();
        if (!config.allowedRemotePorts().contains(port)) {
            throw invalid("remote port is not present in allowed-remote-ports");
        }

        InetAddress[] addresses = resolve(host);
        if (addresses.length == 0) {
            throw invalid("remote host did not resolve to an address");
        }
        List<String> authorizedCidrs = config.allowedRemoteCidrs();
        for (String cidr : authorizedCidrs) {
            validateCidr(cidr);
        }
        for (InetAddress resolvedAddress : addresses) {
            InetAddress address = normalizeMappedAddress(resolvedAddress);
            if (isMetadataAddress(address)) {
                throw invalid("cloud metadata addresses are never allowed");
            }
            if (isNeverAllowedAddress(address) || isReservedAddress(address)) {
                throw invalid("remote host resolves to a loopback, link-local, unspecified, multicast, or reserved address");
            }
            if (isPrivateAddress(address) && !matchesAnyCidr(address, authorizedCidrs)) {
                throw invalid("remote host resolves to a private address outside allowed-remote-cidrs");
            }
        }

        return new ValidatedTarget(uri, host, port, addresses[0]);
    }

    private static boolean isHostAllowed(String host, List<String> configured) {
        if (configured == null || configured.isEmpty()) {
            return false;
        }
        return configured.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(RateLimitRemoteScriptAddressValidator::normalizeHost)
                .anyMatch(host::equals);
    }

    private static String normalizeHost(String value) {
        String host = value.trim();
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }
        if (host.isBlank() || host.contains("*") || host.contains("%")) {
            throw invalid("wildcard and scoped host values are not allowed");
        }
        if (host.indexOf(':') >= 0) {
            try {
                return InetAddress.getByName(host).getHostAddress().toLowerCase(Locale.ROOT);
            } catch (UnknownHostException exception) {
                throw invalid("invalid IPv6 host literal", exception);
            }
        }
        String ascii;
        try {
            ascii = IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES);
        } catch (IllegalArgumentException exception) {
            throw invalid("invalid DNS host name", exception);
        }
        if (ascii.endsWith(".")) {
            ascii = ascii.substring(0, ascii.length() - 1);
        }
        if (ascii.isBlank()) {
            throw invalid("host name is blank");
        }
        return ascii.toLowerCase(Locale.ROOT);
    }

    private static InetAddress[] resolve(String host) {
        try {
            return InetAddress.getAllByName(host);
        } catch (UnknownHostException exception) {
            throw invalid("remote host DNS resolution failed", exception);
        }
    }

    private static boolean isMetadataHost(String host) {
        return BLOCKED_METADATA_HOSTS.contains(host)
                || host.endsWith(".metadata.internal");
    }

    private static boolean isMetadataAddress(InetAddress address) {
        byte[] bytes = address.getAddress();
        if (bytes.length == 16) {
            return matchesIpv6(bytes, "fd00:ec2::254")
                    || matchesIpv6(bytes, "fd20:ce::254");
        }
        int first = Byte.toUnsignedInt(bytes[0]);
        int second = Byte.toUnsignedInt(bytes[1]);
        int third = Byte.toUnsignedInt(bytes[2]);
        int fourth = Byte.toUnsignedInt(bytes[3]);
        return (first == 169 && second == 254 && third == 169 && fourth == 254)
                || (first == 100 && second == 100 && third == 100 && fourth == 200)
                || (first == 168 && second == 63 && third == 129 && fourth == 16);
    }

    private static boolean matchesIpv6(byte[] candidate, String literal) {
        try {
            return java.util.Arrays.equals(candidate, InetAddress.getByName(literal).getAddress());
        } catch (UnknownHostException exception) {
            throw new IllegalStateException("Unable to parse a built-in cloud metadata address", exception);
        }
    }

    private static InetAddress normalizeMappedAddress(InetAddress address) {
        byte[] bytes = address.getAddress();
        if (bytes.length != 16 || !isIpv4Mapped(bytes)) {
            return address;
        }
        try {
            return InetAddress.getByAddress(new byte[]{bytes[12], bytes[13], bytes[14], bytes[15]});
        } catch (UnknownHostException exception) {
            throw new IllegalStateException("Unable to inspect IPv4-mapped IPv6 address", exception);
        }
    }

    private static boolean isNeverAllowedAddress(InetAddress address) {
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isMulticastAddress();
    }

    private static boolean isPrivateAddress(InetAddress address) {
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            return address.isSiteLocalAddress();
        }
        return bytes.length == 16 && (Byte.toUnsignedInt(bytes[0]) & 0xFE) == 0xFC;
    }

    private static boolean isReservedAddress(InetAddress address) {
        byte[] bytes = address.getAddress();
        if (bytes.length == 16) {
            int first = Byte.toUnsignedInt(bytes[0]);
            int second = Byte.toUnsignedInt(bytes[1]);
            boolean documentation = first == 0x20 && second == 0x01
                    && Byte.toUnsignedInt(bytes[2]) == 0x0D
                    && Byte.toUnsignedInt(bytes[3]) == 0xB8;
            boolean deprecatedSiteLocal = addressSiteLocalIpv6(address)
                    && !isPrivateAddress(address);
            return documentation || deprecatedSiteLocal;
        }

        int first = Byte.toUnsignedInt(bytes[0]);
        int second = Byte.toUnsignedInt(bytes[1]);
        int third = Byte.toUnsignedInt(bytes[2]);
        return first == 0
                || first >= 240
                || (first == 100 && second >= 64 && second <= 127)
                || (first == 192 && second == 0 && third == 0)
                || (first == 192 && second == 0 && third == 2)
                || (first == 198 && (second == 18 || second == 19))
                || (first == 198 && second == 51 && third == 100)
                || (first == 203 && second == 0 && third == 113);
    }

    private static boolean addressSiteLocalIpv6(InetAddress address) {
        return address.getAddress().length == 16 && address.isSiteLocalAddress();
    }

    private static boolean isIpv4Mapped(byte[] bytes) {
        for (int index = 0; index < 10; index++) {
            if (bytes[index] != 0) {
                return false;
            }
        }
        return bytes[10] == (byte) 0xFF && bytes[11] == (byte) 0xFF;
    }

    private static void validateCidr(String cidr) {
        parseCidr(cidr);
    }

    private static boolean matchesAnyCidr(InetAddress address, List<String> cidrs) {
        return cidrs.stream().anyMatch(cidr -> contains(parseCidr(cidr), address));
    }

    private static Cidr parseCidr(String value) {
        if (value == null || value.isBlank()) {
            throw invalid("authorized CIDR must not be blank");
        }
        String[] parts = value.trim().split("/", -1);
        if (parts.length > 2 || parts[0].contains("%")) {
            throw invalid("authorized CIDR is malformed");
        }
        String literal = parts[0];
        if (!literal.matches("[0-9a-fA-F:.]+")) {
            throw invalid("authorized CIDR must contain an IP literal");
        }
        try {
            InetAddress address = normalizeMappedAddress(InetAddress.getByName(literal));
            int maxPrefix = address.getAddress().length * Byte.SIZE;
            int minimumPrefix = address.getAddress().length == 4 ? 8 : 7;
            int prefix = maxPrefix;
            if (parts.length == 2) {
                prefix = Integer.parseInt(parts[1]);
            }
            if (prefix < minimumPrefix || prefix > maxPrefix || !isPrivateAddress(address)) {
                throw invalid("authorized CIDR must be a specific RFC1918 or IPv6 ULA network");
            }
            return new Cidr(address.getAddress(), prefix);
        } catch (UnknownHostException | NumberFormatException exception) {
            throw invalid("authorized CIDR is malformed", exception);
        }
    }

    private static boolean contains(Cidr cidr, InetAddress candidate) {
        byte[] address = cidr.address();
        byte[] candidateBytes = candidate.getAddress();
        if (address.length != candidateBytes.length) {
            return false;
        }
        int fullBytes = cidr.prefixLength() / Byte.SIZE;
        int remainingBits = cidr.prefixLength() % Byte.SIZE;
        for (int index = 0; index < fullBytes; index++) {
            if (address[index] != candidateBytes[index]) {
                return false;
            }
        }
        if (remainingBits == 0) {
            return true;
        }
        int mask = 0xFF << (Byte.SIZE - remainingBits);
        return (address[fullBytes] & mask) == (candidateBytes[fullBytes] & mask);
    }

    private static IllegalArgumentException invalid(String reason) {
        return new IllegalArgumentException("Invalid remote rate limit script destination: " + reason);
    }

    private static IllegalArgumentException invalid(String reason, Exception cause) {
        return new IllegalArgumentException("Invalid remote rate limit script destination: " + reason, cause);
    }

    record ValidatedTarget(URI uri, String host, int port, InetAddress address) {
    }

    private record Cidr(byte[] address, int prefixLength) {
    }
}
