package com.peach.response;

/**
 * 中止分片上传结果。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/18 14:15
 */
public class AbortMultipartUploadResult {

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
     * 是否中止成功。
     */
    private final boolean aborted;

    public AbortMultipartUploadResult(String providerName, String bucketName, String objectKey,
                                      String uploadId, boolean aborted) {
        this.providerName = providerName;
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.uploadId = uploadId;
        this.aborted = aborted;
    }


    public String getProviderName() { return providerName; }


    public String getBucketName() { return bucketName; }


    public String getObjectKey() { return objectKey; }


    public String getUploadId() { return uploadId; }


    public boolean isAborted() { return aborted; }
}