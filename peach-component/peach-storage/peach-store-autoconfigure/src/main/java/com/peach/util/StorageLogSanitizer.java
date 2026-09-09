package com.peach.util;

import com.peach.common.util.desensitize.DesensitizeUtil;
import com.peach.config.StorageProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * StorageLogSanitizer相关类。
 * <p>用于输出 provider 配置摘要时隐藏 endpoint、domain、rootPath 等敏感信息。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
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
                + ", endpoint='" + DesensitizeUtil.maskEndpoint(provider.getEndpoint()) + '\''
                + ", region='" + safe(provider.getRegion()) + '\''
                + ", rootPath='" + DesensitizeUtil.maskStoragePath(provider.getRootPath()) + '\''
                + ", domain='" + DesensitizeUtil.maskEndpoint(provider.getDomain()) + '\''
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
