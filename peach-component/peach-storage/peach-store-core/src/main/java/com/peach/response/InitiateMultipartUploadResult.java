package com.peach.response;

/**
 * 初始化分片上传结果。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/18 14:15
 */
public class InitiateMultipartUploadResult {

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

    public InitiateMultipartUploadResult(String providerName, String bucketName, String objectKey, String uploadId) {
        this.providerName = providerName;
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.uploadId = uploadId;
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
}