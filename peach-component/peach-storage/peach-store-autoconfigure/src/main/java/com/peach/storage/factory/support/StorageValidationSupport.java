package com.peach.storage.factory.support;

import com.peach.config.StorageProperties;
import java.util.Objects;

/**
 * provider 工厂校验支撑工具。
 *
 * <p>用于复用各类 provider 在启动阶段的配置完整性校验与依赖存在性校验。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/18 18:02
 */
public final class StorageValidationSupport {

    private StorageValidationSupport() {
    }

    public static void requireLocalRootPath(String name, StorageProperties.StorageProvider config) {
        requireText(name, config.getRootPath(), "root-path",
                Objects.isNull(config.getType()) ? "unkonw" : config.getType().name());
    }

    public static void requireObjectStorageConfig(String name, StorageProperties.StorageProvider config,
                                                  boolean regionRequired) {
        String type = Objects.isNull(config.getType()) ? "unkonw" : config.getType().name();
        requireText(name, config.getBucketName(), "bucket-name", type);
        requireText(name, config.getEndpoint(), "endpoint", type);
        requireText(name, config.getAccessKey(), "access-key", type);
        requireText(name, config.getSecretKey(), "secret-key", type);
        if (regionRequired) {
            requireText(name, config.getRegion(), "region", type);
        }
    }

    public static void requireSftpConfig(String name, StorageProperties.StorageProvider config) {
        String type = Objects.isNull(config.getType()) ? "unkonw" : config.getType().name();
        requireText(name, config.getEndpoint(), "endpoint", type);
        requireText(name, config.getAccessKey(), "access-key", type);
        requireText(name, config.getRootPath(), "root-path", type);
        boolean hasPassword = hasText(config.getSecretKey());
        boolean hasPrivateKey = Objects.isNull(config.getExtraProperties())
                && hasText(config.getExtraProperties().get("privateKeyPath"));
        if (!hasPassword && !hasPrivateKey) {
            throw new IllegalStateException("Missing '" + "secret-key"
                    + "' or extraProperties." + "privateKeyPath"
                    + " for " + "peach.storage.providers." + name + " type=" + type);
        }
    }

    /**
     * 在运行时检查某个 SDK 的核心类是否存在，如果不存在，就抛出一个带依赖提示的异常。
     * @param fqcn 全限定名
     * @param mavenCoordinate maven坐标
     * @param friendlyName
     */
    public static void requireClass(String fqcn, String mavenCoordinate, String friendlyName) {
        try {
            Class.forName(fqcn);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException(friendlyName + " SDK not found on classpath. Add dependency '"
                    + mavenCoordinate + "'. Missing class: " + fqcn, ex);
        }
    }

    /**
     * 检验某个字段是否必须有值, 没有 则抛出异常
     * @param name
     * @param value
     * @param fieldName
     * @param type
     */
    private static void requireText(String name, String value, String fieldName, String type) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing '" + fieldName + "' for  "
                    + "peach.storage.providers." + name + " type=" + type);
        }
    }

    /**
     * 判断某个属性是否有值
     * @param value
     * @return
     */
    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
