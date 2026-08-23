package com.peach.config;

import com.peach.enums.StorageType;
import org.springframework.validation.annotation.Validated;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Peach Storage starter 配置项。
 *
 * <p>配置前缀为 {@code peach.storage}。一个应用可以同时配置多个存储实例，
 * 例如一个默认 OSS 桶、一个私有 S3 桶和一个用于临时文件的本地/NAS 目录。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/10 15:11
 */
@Validated
@ConfigurationProperties(prefix = "peach.storage")
public class StorageProperties {

    /**
     * 是否启用 starter 自动装配。
     */
    private boolean enabled = true;

    /**
     * 默认存储实例名称。业务调用 StorageService 未指定 providerName 时使用该实例。
     */
    private String primary;

    /**
     * 多存储实例配置列表。
     */
    private Map<String, StorageProvider> providers = new LinkedHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPrimary() {
        return primary;
    }

    public void setPrimary(String primary) {
        this.primary = primary;
    }

    public Map<String, StorageProvider> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, StorageProvider> providers) {
        this.providers = providers == null ? new LinkedHashMap<>() : providers;
    }

    /**
     * 启动期通用配置校验。
     */
    public void validateForStartup() {
        if (!enabled) {
            return;
        }
        if (primary == null || primary.isBlank()) {
            throw new IllegalStateException("Property '" + "peach.storage.primary"
                    + "' must not be blank when storage is enabled");
        }
        if (providers == null || providers.isEmpty()) {
            throw new IllegalStateException("Property '" + "peach.storage.providers"
                    + "' must not be empty when storage is enabled");
        }
        if (!providers.containsKey(primary.trim())) {
            throw new IllegalStateException("peach.storage.primary" + "=[" + primary
                    + "] does not match any provider. Available providers=" + providers.keySet());
        }
    }

    /**
     * 单个存储实例配置。
     */
    public static class StorageProvider {

        /**
         * 实例名称，必须全局唯一。例如 default、archive、local-temp。
         */
        private String name;

        /**
         * 存储类型，例如 OSS、OBS、S3、COS、BOS、CEPH、MINIO、NAS、LOCAL。
         */
        private StorageType type;

        /**
         * 存储桶名称或逻辑存储别名。
         *
         * <p>对象存储类型表示真实 bucket；LOCAL/NAS/SFTP 这类无物理 bucket 的存储类型中，
         * 该字段只作为展示与校验用的逻辑别名，真实边界由 `rootPath` 决定。</p>
         */
        private String bucketName;

        /**
         * 对象 key 统一前缀，例如 app-a/prod。实际读写时会自动拼接到 objectKey 前。
         */
        private String prefix;

        /**
         * 对象存储 endpoint，例如 https://oss-cn-hangzhou.aliyuncs.com。
         */
        private String endpoint;

        /**
         * 存储区域，例如 cn-hangzhou、ap-shanghai、us-east-1。
         */
        private String region;

        /**
         * 访问密钥 ID。生产环境建议通过环境变量或配置中心注入。
         */
        private String accessKey;

        /**
         * 访问密钥 Secret。生产环境建议通过环境变量或配置中心注入。
         */
        private String secretKey;

        /**
         * 本地、NAS 或 SFTP 根目录。无物理 bucket 的存储类型以此字段作为真实存储边界。
         */
        private String rootPath;

        /**
         * 自定义访问域名，用于生成公开访问 URL。
         */
        private String domain;

        /**
         * 是否启用 path-style 访问。S3、MinIO、Ceph 场景常用。
         */
        private boolean pathStyleAccess;

        /**
         * 上传后是否默认设置公共读，具体是否生效由 provider 决定。
         */
        private boolean publicRead;

        /**
         * 厂商 SDK 扩展参数，starter 不解析，交由具体 SPI 实现读取。
         */
        private Map<String, String> extraProperties = new LinkedHashMap<>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public StorageType getType() {
            return type;
        }

        public void setType(StorageType type) {
            this.type = type;
        }

        public String getBucketName() {
            return bucketName;
        }

        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getRootPath() {
            return rootPath;
        }

        public void setRootPath(String rootPath) {
            this.rootPath = rootPath;
        }

        public String getDomain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }

        public boolean isPathStyleAccess() {
            return pathStyleAccess;
        }

        public void setPathStyleAccess(boolean pathStyleAccess) {
            this.pathStyleAccess = pathStyleAccess;
        }

        public boolean isPublicRead() {
            return publicRead;
        }

        public void setPublicRead(boolean publicRead) {
            this.publicRead = publicRead;
        }

        public Map<String, String> getExtraProperties() {
            return extraProperties;
        }

        public void setExtraProperties(Map<String, String> extraProperties) {
            this.extraProperties = extraProperties == null ? new LinkedHashMap<>() : extraProperties;
        }
    }
}
