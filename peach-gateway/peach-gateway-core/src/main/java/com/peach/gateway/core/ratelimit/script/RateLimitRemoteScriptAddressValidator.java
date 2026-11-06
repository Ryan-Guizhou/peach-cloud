package com.peach.gateway.core.ratelimit.script;

import com.peach.gateway.core.ratelimit.config.GatewayRateLimitProperties;

import java.net.IDN;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
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
final class RateLimitRemoteScriptAddressValidator {

    private static final List<String> BLOCKED_METADATA_HOSTS = List.of(
            "metadata",
            "metadata.google.internal",
            "metadata.azure.com",
            "instance-data.ec2.internal");

    private RateLimitRemoteScriptAddressValidator() {
    }

    static ValidatedTarget validate(String value, GatewayRateLimitProperties.ScriptProperties config) {
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
        if ((!https && !http) || (config.isRequireHttps() && !https)
                || uri.getHost() == null || uri.getHost().isBlank()
                || uri.getUserInfo() != null || uri.getFragment() != null) {
            throw invalid("remote URL must be an approved absolute HTTP(S) URL without user-info or fragment");
        }

        String host = normalizeHost(uri.getHost());
        if (isMetadataHost(host)) {
            throw invalid("cloud metadata hosts are never allowed");
        }
        if (!isHostAllowed(host, config.getAllowedRemoteHosts())) {
            throw invalid("remote host is not present in allowed-remote-hosts");
        }

        int port = uri.getPort() < 0 ? (https ? 443 : 80) : uri.getPort();
        if (config.getAllowedRemotePorts() == null || !config.getAllowedRemotePorts().contains(port)) {
            throw invalid("remote port is not present in allowed-remote-ports");
        }

        InetAddress[] addresses = resolve(host);
        if (addresses.length == 0) {
            throw invalid("remote host did not resolve to an address");
        }
        List<String> authorizedCidrs = config.getAllowedRemoteCidrs() == null
                ? List.of() : List.copyOf(config.getAllowedRemoteCidrs());
        for (String cidr : authorizedCidrs) {
            validateCidr(cidr);
        }
        for (InetAddress address : addresses) {
            if (isMetadataAddress(address)) {
                throw invalid("cloud metadata addresses are never allowed");
            }
            if (isBlockedAddress(address) && !matchesAnyCidr(address, authorizedCidrs)) {
                throw invalid("remote host resolves to a private, local, or reserved address");
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
        if (bytes.length != 4) {
            return false;
        }
        int first = Byte.toUnsignedInt(bytes[0]);
        int second = Byte.toUnsignedInt(bytes[1]);
        int third = Byte.toUnsignedInt(bytes[2]);
        int fourth = Byte.toUnsignedInt(bytes[3]);
        return (first == 169 && second == 254 && third == 169 && fourth == 254)
                || (first == 100 && second == 100 && third == 100 && fourth == 200)
                || (first == 168 && second == 63 && third == 129 && fourth == 16);
    }

    private static boolean isBlockedAddress(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 16) {
            if (isIpv4Mapped(bytes)) {
                try {
                    return isBlockedAddress(InetAddress.getByAddress(
                            new byte[]{bytes[12], bytes[13], bytes[14], bytes[15]}));
                } catch (UnknownHostException exception) {
                    throw new IllegalStateException("Unable to inspect IPv4-mapped IPv6 address", exception);
                }
            }
            int first = Byte.toUnsignedInt(bytes[0]);
            int second = Byte.toUnsignedInt(bytes[1]);
            boolean uniqueLocal = (first & 0xFE) == 0xFC;
            boolean documentation = first == 0x20 && second == 0x01
                    && Byte.toUnsignedInt(bytes[2]) == 0x0D
                    && Byte.toUnsignedInt(bytes[3]) == 0xB8;
            return uniqueLocal || documentation;
        }

        int first = Byte.toUnsignedInt(bytes[0]);
        int second = Byte.toUnsignedInt(bytes[1]);
        int third = Byte.toUnsignedInt(bytes[2]);
        return first == 0
                || first >= 224
                || (first == 100 && second >= 64 && second <= 127)
                || (first == 192 && second == 0 && third == 0)
                || (first == 192 && second == 0 && third == 2)
                || (first == 198 && (second == 18 || second == 19))
                || (first == 198 && second == 51 && third == 100)
                || (first == 203 && second == 0 && third == 113);
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
            InetAddress address = InetAddress.getByName(literal);
            int maxPrefix = address.getAddress().length * Byte.SIZE;
            int prefix = maxPrefix;
            if (parts.length == 2) {
                prefix = Integer.parseInt(parts[1]);
            }
            if (prefix < 0 || prefix > maxPrefix) {
                throw invalid("authorized CIDR prefix is outside the address range");
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
