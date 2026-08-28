package com.peach.response;

import java.time.Instant;

/**
 * 上传Part结果。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/18 14:15
 */
public class UploadPartResult {

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
     * 分片上传会话标识。
     */
    private final String uploadId;

    /**
     * 分片序号。
     */
    private final int partNumber;

    /**
     * 当前分片对应的上传 URL。
     */
    private final String url;

    /**
     * 当前分片上传 URL 的过期时间。
     */
    private final Instant expiresAt;

    public UploadPartResult(String providerName, String bucketName, String objectKey, String uploadId,
                            int partNumber, String url, Instant expiresAt) {
        this.providerName = providerName;
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.uploadId = uploadId;
        this.partNumber = partNumber;
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


    public String getUploadId() {
        return uploadId;
    }


    public int getPartNumber() {
        return partNumber;
    }


    public String getUrl() {
        return url;
    }


    public Instant getExpiresAt() {
        return expiresAt;
    }
}