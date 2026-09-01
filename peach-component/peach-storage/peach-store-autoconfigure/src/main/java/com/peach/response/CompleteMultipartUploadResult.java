package com.peach.response;

/**
 * CompleteMultipart上传结果。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/18 14:15
 */
public class CompleteMultipartUploadResult {

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
     * 上传完成后返回的 ETag。
     */
    private final String eTag;

    /**
     * 对象访问地址。
     */
    private final String location;

    public CompleteMultipartUploadResult(String providerName, String bucketName, String objectKey,
                                         String uploadId, String eTag, String location) {
        this.providerName = providerName;
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.uploadId = uploadId;
        this.eTag = eTag;
        this.location = location;
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


    public String getETag() {
        return eTag;
    }


    public String getLocation() {
        return location;
    }
}
