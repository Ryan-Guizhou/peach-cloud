package com.peach.request;

import com.peach.enums.StorageResultCode;
import com.peach.exception.StorageException;

/**
 * 查询对象列表请求。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/16 14:01
 */
public class ListObjectsRequest {

    /**
     * 目标 bucket 名称；为空时使用 provider 默认 bucket。
     */
    private final String bucketName;

    /**
     * 列表查询前缀。
     */
    private final String prefix;

    /**
     * 单次返回的最大对象数。
     */
    private final int maxKeys;

    /**
     * 分页续传游标。
     */
    private final String continuationToken;

    /**
     * 目录分隔符。
     */
    private final String delimiter;

    /**
     * 是否递归列出全部对象。
     */
    private final boolean recursive;

    private ListObjectsRequest(Builder builder) {
        this.bucketName = builder.bucketName;
        this.prefix = builder.prefix;
        this.maxKeys = builder.maxKeys;
        this.continuationToken = builder.continuationToken;
        this.delimiter = builder.delimiter;
        this.recursive = builder.recursive;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getPrefix() {
        return prefix;
    }

    public int getMaxKeys() {
        return maxKeys;
    }

    public String getContinuationToken() {
        return continuationToken;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public static class Builder {

        private String bucketName;
        private String prefix;
        private int maxKeys = 1000;
        private String continuationToken;
        private String delimiter;
        private boolean recursive;

        public Builder bucketName(String bucketName) {
            this.bucketName = bucketName;
            return this;
        }

        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder maxKeys(int maxKeys) {
            this.maxKeys = maxKeys;
            return this;
        }

        public Builder continuationToken(String continuationToken) {
            this.continuationToken = continuationToken;
            return this;
        }

        public Builder delimiter(String delimiter) {
            this.delimiter = delimiter;
            return this;
        }

        public Builder recursive(boolean recursive) {
            this.recursive = recursive;
            if (recursive && (this.delimiter == null || this.delimiter.isBlank())) {
                this.delimiter = "/";
            }
            return this;
        }

        public ListObjectsRequest build() {
            if (maxKeys <= 0) {
                throw new StorageException(StorageResultCode.BAD_REQUEST, "Max keys must be greater than 0");
            }
            if (delimiter != null && delimiter.isBlank()) {
                throw new StorageException(StorageResultCode.BAD_REQUEST, "Delimiter must not be blank");
            }
            return new ListObjectsRequest(this);
        }
    }
}
