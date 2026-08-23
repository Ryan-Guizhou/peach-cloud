package com.peach.request;

import com.peach.enums.StorageResultCode;
import com.peach.exception.StorageException;

/**
 * 源对象到目标对象的传输请求基类。
 *
 * <p>用于拷贝、移动这类同时依赖源路径和目标路径的操作。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/17 16:20
 */
public class StorageTransferRequest {

    /**
     * 源 bucket 名称；为空时使用 provider 默认 bucket。
     */
    private final String sourceBucketName;

    /**
     * 源对象 key。
     */
    private final String sourceObjectKey;

    /**
     * 目标 bucket 名称；为空时使用 provider 默认 bucket。
     */
    private final String targetBucketName;

    /**
     * 目标对象 key。
     */
    private final String targetObjectKey;

    /**
     * 是否按目录递归处理。
     */
    private final boolean recursive;

    /**
     * 目标对象已存在时是否允许覆盖。
     */
    private final boolean overwrite;

    protected StorageTransferRequest(Builder<?> builder) {
        this.sourceBucketName = builder.sourceBucketName;
        this.sourceObjectKey = builder.sourceObjectKey;
        this.targetBucketName = builder.targetBucketName;
        this.targetObjectKey = builder.targetObjectKey;
        this.recursive = builder.recursive;
        this.overwrite = builder.overwrite;
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

    public boolean isRecursive() {
        return recursive;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public static Builder<?> builder() {
        return new Builder<>();
    }

    public static class Builder<T extends Builder<T>> {

        private String sourceBucketName;
        private String sourceObjectKey;
        private String targetBucketName;
        private String targetObjectKey;
        private boolean recursive;
        private boolean overwrite;

        public T sourceBucketName(String sourceBucketName) {
            this.sourceBucketName = sourceBucketName;
            return self();
        }

        public T sourceObjectKey(String sourceObjectKey) {
            this.sourceObjectKey = sourceObjectKey;
            return self();
        }

        public T targetBucketName(String targetBucketName) {
            this.targetBucketName = targetBucketName;
            return self();
        }

        public T targetObjectKey(String targetObjectKey) {
            this.targetObjectKey = targetObjectKey;
            return self();
        }

        public T recursive(boolean recursive) {
            this.recursive = recursive;
            return self();
        }

        public T overwrite(boolean overwrite) {
            this.overwrite = overwrite;
            return self();
        }

        protected void validate() {
            if (sourceObjectKey == null || sourceObjectKey.isBlank()) {
                throw new StorageException(StorageResultCode.BAD_REQUEST, "Source object key must not be blank");
            }
            if (targetObjectKey == null || targetObjectKey.isBlank()) {
                throw new StorageException(StorageResultCode.BAD_REQUEST, "Target object key must not be blank");
            }
            if (sourceObjectKey.trim().equals(targetObjectKey.trim()) && sameBucket(sourceBucketName, targetBucketName)) {
                throw new StorageException(StorageResultCode.BAD_REQUEST,
                        "Source object key and target object key must not be the same");
            }
        }

        private boolean sameBucket(String sourceBucketName, String targetBucketName) {
            String source = sourceBucketName == null ? null : sourceBucketName.trim();
            String target = targetBucketName == null ? null : targetBucketName.trim();
            return source == null ? target == null : source.equals(target);
        }

        @SuppressWarnings("unchecked")
        protected T self() {
            return (T) this;
        }

        public StorageTransferRequest build() {
            validate();
            return new StorageTransferRequest(this);
        }
    }
}