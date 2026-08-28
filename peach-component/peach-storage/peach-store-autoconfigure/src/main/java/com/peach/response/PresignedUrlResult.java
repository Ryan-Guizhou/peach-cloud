package com.peach.response;

import java.time.Instant;


/**
 * PresignedUrl结果。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/16 14:01
 */
public class PresignedUrlResult {

    /**
     * 存储提供者名称。
     */
    private final String providerName;

    /**
     * 存储桶名称。
     */
    private final String bucketName;

    /**
     * 对象 key。
     */
    private final String objectKey;

    /**
     * 预签名 URL。
     */
    private final String url;

    /**
     * URL 过期时间。
     */
    private final Instant expiresAt;

    public PresignedUrlResult(String providerName, String bucketName, String objectKey, String url, Instant expiresAt) {
        this.providerName = providerName;
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.url = url;
        this.expiresAt = expiresAt;
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

    public String getUrl() {
        return url;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
