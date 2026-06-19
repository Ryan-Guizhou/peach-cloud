package com.peach.response;

/**
 * 移动对象结果。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/17 16:20
 */
public class MoveResult {

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
     * 是否移动成功。
     */
    private final boolean moved;

    public MoveResult(String providerName, String sourceBucketName, String sourceObjectKey,
                      String targetBucketName, String targetObjectKey, boolean moved) {
        this.providerName = providerName;
        this.sourceBucketName = sourceBucketName;
        this.sourceObjectKey = sourceObjectKey;
        this.targetBucketName = targetBucketName;
        this.targetObjectKey = targetObjectKey;
        this.moved = moved;
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

    public boolean isMoved() {
        return moved;
    }
}
