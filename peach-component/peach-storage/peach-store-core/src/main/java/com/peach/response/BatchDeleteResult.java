package com.peach.response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 批量删除对象结果。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/17 16:20
 */
public class BatchDeleteResult {

    /**
     * 存储提供者名称。
     */
    private final String providerName;

    /**
     * 存储桶名称。
     */
    private final String bucketName;

    /**
     * 本次参与删除的对象 key 集合。
     */
    private final List<String> objectKeys;

    /**
     * 实际删除成功的对象数量。
     */
    private final int deletedCount;

    public BatchDeleteResult(String providerName, String bucketName, List<String> objectKeys, int deletedCount) {
        this.providerName = providerName;
        this.bucketName = bucketName;
        this.objectKeys = Collections.unmodifiableList(new ArrayList<>(objectKeys));
        this.deletedCount = deletedCount;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getBucketName() {
        return bucketName;
    }

    public List<String> getObjectKeys() {
        return objectKeys;
    }

    public int getDeletedCount() {
        return deletedCount;
    }
}
