package com.peach.request;


import com.peach.enums.StorageResultCode;
import com.peach.exception.StorageException;

/**
 * 单对象操作基础请求。
 *
 * <p>`bucketName` 可以为空。对象存储类型会使用默认 bucket；LOCAL/NAS/SFTP 这类无物理 bucket
 * 的 provider 会使用固定 alias，并以配置的 `rootPath` 作为真实边界。`objectKey` 必须是对象
 * 存储 key，不建议业务层传入本地绝对路径。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/16 14:01
 */
public class StorageObjectRequest {

    /**
     * 目标 bucket 名称；为空时使用 provider 默认 bucket。
     */
    private final String bucketName;

    /**
     * 对象 key。
     */
    private final String objectKey;

    protected StorageObjectRequest(Builder<?> builder) {
        this.bucketName = builder.bucketName;
        this.objectKey = builder.objectKey;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public static Builder<?> builder() {
        return new Builder<>();
    }

    public static class Builder<T extends Builder<T>> {

        private String bucketName;
        private String objectKey;

        public T bucketName(String bucketName) {
            this.bucketName = bucketName;
            return self();
        }

        public T objectKey(String objectKey) {
            this.objectKey = objectKey;
            return self();
        }

        protected void validate() {
            if (objectKey == null || objectKey.trim().isEmpty()) {
                throw new StorageException(StorageResultCode.BAD_REQUEST, "Object key must not be blank");
            }
        }

        @SuppressWarnings("unchecked")
        protected T self() {
            return (T) this;
        }

        public StorageObjectRequest build() {
            validate();
            return new StorageObjectRequest(this);
        }
    }
}