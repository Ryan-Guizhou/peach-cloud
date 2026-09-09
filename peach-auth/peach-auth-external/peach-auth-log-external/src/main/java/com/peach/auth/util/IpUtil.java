package com.peach.auth.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
public final class IpUtil {

    private static final String IP_UTILS_FLAG = ",";

    private static final String UNKNOWN = "unknown";

    private static final String LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";

    private static final String LOCALHOST_IPV4 = "127.0.0.1";

    private static final String IPV4_REGEX = "^(\\d{1,3}\\.){3}\\d{1,3}$";

    private IpUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean isValidIpv4(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        if (!ip.matches(IPV4_REGEX)) {
            return false;
        }
        String[] parts = ip.split("\\.");
        for (String part : parts) {
            try {
                int value = Integer.parseInt(part);
                if (value < 0 || value > 255) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    public static String getIpAddr(HttpServletRequest request) {
        String ip = null;
        try {
            ip = resolveForwardedIp(request);
            if (StringUtils.isEmpty(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
                ip = resolveLocalHostIp(request);
            }
        } catch (RuntimeException e) {
            log.warn("Failed to resolve client IP, reason={}", e.getClass().getSimpleName());
        }
        ip = firstForwardedIp(ip);
        if (isValidIpv4(ip)) {
            return ip;
        }
        return null;
    }

    private static String resolveForwardedIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Original-Forwarded-For");
        if (StringUtils.isEmpty(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (StringUtils.isEmpty(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("x-forwarded-for");
        }
        if (StringUtils.isEmpty(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StringUtils.isEmpty(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StringUtils.isEmpty(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (StringUtils.isEmpty(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        return ip;
    }

    private static String resolveLocalHostIp(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        if (!LOCALHOST_IPV6.equalsIgnoreCase(ip) && !LOCALHOST_IPV4.equalsIgnoreCase(ip)) {
            return ip;
        }
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("Failed to resolve local host address, reason={}", e.getClass().getSimpleName());
            return ip;
        }
    }

    private static String firstForwardedIp(String ip) {
        if (!StringUtils.isEmpty(ip) && ip.indexOf(IP_UTILS_FLAG) >= 0) {
            return ip.substring(0, ip.indexOf(IP_UTILS_FLAG));
        }
        return ip;
    }
}
