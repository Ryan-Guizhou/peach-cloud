package com.peach.response;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 存储对象元信息。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/16 14:01
 */
public class ObjectInfo {

    /**
     * 存储提供者名称。
     */
    private final String providerName;

    /**
     * 存储桶名称。
     */
    private final String bucketName;

    /**
     * 对象 key，例如：`peachsoft/cloud/hello.txt`。
     */
    private final String objectKey;

    /**
     * 对象大小，单位字节。
     */
    private final long size;

    /**
     * 对象内容类型。
     */
    private final String contentType;

    /**
     * 对象 ETag。
     */
    private final String etag;

    /**
     * 最后修改时间。
     */
    private final Instant lastModified;

    /**
     * 对象元数据。
     */
    private final Map<String, String> metadata;

    private ObjectInfo(Builder builder) {
        this.providerName = builder.providerName;
        this.bucketName = builder.bucketName;
        this.objectKey = builder.objectKey;
        this.size = builder.size;
        this.contentType = builder.contentType;
        this.etag = builder.etag;
        this.lastModified = builder.lastModified;
        this.metadata = Collections.unmodifiableMap(new LinkedHashMap<>(builder.metadata));
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getProviderName() {
        return providerName;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public long getSize() {
        return size;
    }

    public String getContentType() {
        return contentType;
    }

    public String getEtag() {
        return etag;
    }

    public Instant getLastModified() {
        return lastModified;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public static class Builder {

        private String providerName;
        private String bucketName;
        private String objectKey;
        private long size = -1L;
        private String contentType;
        private String etag;
        private Instant lastModified;
        private Map<String, String> metadata = new LinkedHashMap<>();

        public Builder providerName(String providerName) {
            this.providerName = providerName;
            return this;
        }

        public Builder bucketName(String bucketName) {
            this.bucketName = bucketName;
            return this;
        }

        public Builder objectKey(String objectKey) {
            this.objectKey = objectKey;
            return this;
        }

        public Builder size(long size) {
            this.size = size;
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder etag(String etag) {
            this.etag = etag;
            return this;
        }

        public Builder lastModified(Instant lastModified) {
            this.lastModified = lastModified;
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
            return this;
        }

        public ObjectInfo build() {
            return new ObjectInfo(this);
        }
    }
}