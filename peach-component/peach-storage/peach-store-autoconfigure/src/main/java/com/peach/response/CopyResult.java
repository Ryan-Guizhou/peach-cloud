package com.peach.response;

/**
 * Copy结果。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/17 16:20
 */
public class CopyResult {

    /**
     * 存储提供者名称。
     */
    private final String providerName;

    /**
     * 源存储桶名称。
     */
    private final String sourceBucketName;

    /**
     * 源对象 key。
     */
    private final String sourceObjectKey;

    /**
     * 目标存储桶名称。
     */
    private final String targetBucketName;

    /**
     * 目标对象 key。
     */
    private final String targetObjectKey;

    /**
     * 是否拷贝成功。
     */
    private final boolean copied;

    public CopyResult(String providerName, String sourceBucketName, String sourceObjectKey,
                      String targetBucketName, String targetObjectKey, boolean copied) {
        this.providerName = providerName;
        this.sourceBucketName = sourceBucketName;
        this.sourceObjectKey = sourceObjectKey;
        this.targetBucketName = targetBucketName;
        this.targetObjectKey = targetObjectKey;
        this.copied = copied;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getSourceBucketName() {
        return sourceBucketName;
    }

    public String getSourceObjectKey() {
        return sourceObjectKey;
    }

    public String getTargetBucketName() {
        return targetBucketName;
    }

    public String getTargetObjectKey() {
        return targetObjectKey;
    }

    public boolean isCopied() {
        return copied;
    }
}
