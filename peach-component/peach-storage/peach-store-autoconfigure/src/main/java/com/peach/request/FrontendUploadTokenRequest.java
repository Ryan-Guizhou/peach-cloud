package com.peach.request;

import com.peach.enums.StorageResultCode;
import com.peach.exception.StorageException;

/**
 * FrontendUploadTokenRequest相关类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/18 14:15
 */
public class FrontendUploadTokenRequest extends StorageObjectRequest {

    /**
     * 令牌有效期，单位秒。
     */
    private final long expireSeconds;

    /**
     * 前端允许上传的最大对象大小，单位字节。
     */
    private final long maxSize;

    private FrontendUploadTokenRequest(Builder builder) {
        super(builder);
        this.expireSeconds = builder.expireSeconds;
        this.maxSize = builder.maxSize;
    }

    /**
     * 创建前端直传令牌请求构造器。
     *
     * @return 构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 获取令牌有效期。
     *
     * @return 有效期，单位秒
     */
    public long getExpireSeconds() {
        return expireSeconds;
    }

    /**
     * 获取前端允许上传的最大对象大小。
     *
     * @return 最大对象大小，单位字节
     */
    public long getMaxSize() {
        return maxSize;
    }

    public static class Builder extends StorageObjectRequest.Builder<Builder> {

        private long expireSeconds = 300L;
        private long maxSize = 1024L * 1024L * 1024L;

        public Builder expireSeconds(long expireSeconds) {
            this.expireSeconds = expireSeconds;
            return this;
        }

        public Builder maxSize(long maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public FrontendUploadTokenRequest build() {
            validate();
            if (expireSeconds <= 0) {
                throw new StorageException(StorageResultCode.BAD_REQUEST, "Expire seconds must be greater than 0");
            }
            if (maxSize <= 0) {
                throw new StorageException(StorageResultCode.BAD_REQUEST, "Max size must be greater than 0");
            }
            return new FrontendUploadTokenRequest(this);
        }
    }
}
