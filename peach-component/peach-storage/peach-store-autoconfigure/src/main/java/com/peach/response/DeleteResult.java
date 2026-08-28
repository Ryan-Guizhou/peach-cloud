package com.peach.response;


/**
 * Delete结果。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/16 14:01
 */
public class DeleteResult {

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
     * 是否删除成功。
     */
    private final boolean deleted;

    public DeleteResult(String providerName, String bucketName, String objectKey, boolean deleted) {
        this.providerName = providerName;
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.deleted = deleted;
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

    public boolean isDeleted() {
        return deleted;
    }
}
