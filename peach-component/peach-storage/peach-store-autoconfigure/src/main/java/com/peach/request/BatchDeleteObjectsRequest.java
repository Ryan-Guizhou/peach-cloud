package com.peach.request;

import com.peach.enums.StorageResultCode;
import com.peach.exception.StorageException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 批量删除对象请求。
 *
 * <p>当前用于删除同一 bucket 下的多个对象 key。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/17 16:20
 */
public class BatchDeleteObjectsRequest {

    /**
     * 目标 bucket 名称；为空时使用 provider 默认 bucket。
     */
    private final String bucketName;

    /**
     * 需要删除的对象 key 集合。
     */
    private final List<String> objectKeys;

    /**
     * 是否启用静默删除模式。
     */
    private final boolean quiet;

    private BatchDeleteObjectsRequest(Builder builder) {
        this.bucketName = builder.bucketName;
        this.objectKeys = Collections.unmodifiableList(new ArrayList<>(builder.objectKeys));
        this.quiet = builder.quiet;
    }

    /**
     * 获取目标 bucket 名称。
     *
     * @return bucket 名称
     */
    public String getBucketName() { return bucketName; }

    /**
     * 获取需要删除的对象 key 集合。
     *
     * @return 对象 key 集合
     */
    public List<String> getObjectKeys() { return objectKeys; }

    /**
     * 判断是否启用静默模式。
     *
     * @return true 表示启用静默模式
     */
    public boolean isQuiet() { return quiet; }

    /**
     * 创建批量删除请求构造器。
     *
     * @return 构造器
     */
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String bucketName;
        private List<String> objectKeys = new ArrayList<>();
        private boolean quiet;

        public Builder bucketName(String bucketName) { this.bucketName = bucketName; return this; }
        public Builder objectKeys(List<String> objectKeys) { this.objectKeys = objectKeys == null ? new ArrayList<String>() : new ArrayList<>(objectKeys); return this; }
        public Builder addObjectKey(String objectKey) { this.objectKeys.add(objectKey); return this; }
        public Builder quiet(boolean quiet) { this.quiet = quiet; return this; }

        public BatchDeleteObjectsRequest build() {
            validate();
            return new BatchDeleteObjectsRequest(this);
        }

        private void validate() {
            if (objectKeys == null || objectKeys.isEmpty()) {
                throw new StorageException(StorageResultCode.BAD_REQUEST, "Object keys must not be empty");
            }
            for (String objectKey : objectKeys) {
                if (objectKey == null || objectKey.isBlank()) {
                    throw new StorageException(StorageResultCode.BAD_REQUEST, "Object key must not be blank");
                }
            }
        }
    }
}