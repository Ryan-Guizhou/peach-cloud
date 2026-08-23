package com.peach.util;

import com.peach.config.StorageProperties;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 存储日志脱敏工具。
 *
 * <p>用于输出 provider 配置摘要时隐藏 endpoint、domain、rootPath 等敏感信息。</p>
 */
public final class StorageLogSanitizer {

    private StorageLogSanitizer() {
    }

    public static String providerSummary(StorageProperties.StorageProvider provider) {
        if (provider == null) {
            return "StorageProviderConfig{null}";
        }
        return "StorageProviderConfig{"
                + "name='" + safe(provider.getName()) + '\''
                + ", type=" + provider.getType()
                + ", bucketName='" + safe(provider.getBucketName()) + '\''
                + ", prefix='" + safe(provider.getPrefix()) + '\''
                + ", endpoint='" + maskEndpoint(provider.getEndpoint()) + '\''
                + ", region='" + safe(provider.getRegion()) + '\''
                + ", rootPath='" + maskPath(provider.getRootPath()) + '\''
                + ", domain='" + maskEndpoint(provider.getDomain()) + '\''
                + ", pathStyleAccess=" + provider.isPathStyleAccess()
                + ", publicRead=" + provider.isPublicRead()
                + ", extraPropertiesKeys=" + mapKeys(provider.getExtraProperties())
                + '}';
    }

    public static String providerNames(Map<String, ?> providers) {
        if (providers == null || providers.isEmpty()) {
            return "[]";
        }
        return new ArrayList<String>(providers.keySet()).toString();
    }

    public static String providerNames(Iterable<String> providerNames) {
        if (providerNames == null) {
            return "[]";
        }
        List<String> names = new ArrayList<String>();
        for (String providerName : providerNames) {
            names.add(providerName);
        }
        return names.toString();
    }

    private static String maskEndpoint(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String trimmed = value.trim();
        try {
            URI uri = URI.create(trimmed.contains("://") ? trimmed : "https://" + trimmed);
            String host = uri.getHost();
            String scheme = uri.getScheme();
            if (host == null || host.isBlank()) {
                return maskPlain(trimmed);
            }
            return (scheme == null ? "" : scheme + "://") + maskHost(host);
        } catch (Exception ex) {
            return maskPlain(trimmed);
        }
    }

    private static String maskPath(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String normalized = value.trim().replace('\\', '/');
        String[] parts = normalized.split("/");
        String last = null;
        for (int i = parts.length - 1; i >= 0; i--) {
            if (parts[i] != null && !parts[i].isBlank()) {
                last = parts[i].trim();
                break;
            }
        }
        if (last == null) {
            return "***";
        }
        return "***" + "/" + last;
    }

    private static String maskHost(String host) {
        String[] labels = host.split("\\.");
        if (labels.length == 0) {
            return maskPlain(host);
        }
        if (labels.length == 1) {
            return maskPlain(host);
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < labels.length; i++) {
            if (i > 0) {
                builder.append('.');
            }
            if (i < labels.length - 2) {
                builder.append("***");
            } else {
                builder.append(labels[i]);
            }
        }
        return builder.toString();
    }

    private static String maskPlain(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.length() <= 4) {
            return "***";
        }
        return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
    }

    private static String safe(String value) {
        return value == null ? null : value.trim();
    }

    private static String mapKeys(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return "[]";
        }
        return new ArrayList<String>(map.keySet()).toString();
    }
}
