package com.peach.request;

import com.peach.enums.StorageResultCode;
import com.peach.exception.StorageException;

/**
 * 预签名 URL 请求。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/16 14:01
 */
public class PresignedUrlRequest extends StorageObjectRequest {

    /**
     * URL 有效期，单位秒。
     */
    private final long expireSeconds;

    private PresignedUrlRequest(Builder builder) {
        super(builder);
        this.expireSeconds = builder.expireSeconds;
    }

    public static Builder builder() {
        return new Builder();
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }

    public static class Builder extends StorageObjectRequest.Builder<Builder> {

        private long expireSeconds = 3600L;

        public Builder expireSeconds(long expireSeconds) {
            this.expireSeconds = expireSeconds;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        public PresignedUrlRequest build() {
            validate();
            if (expireSeconds <= 0) {
                throw new StorageException(StorageResultCode.BAD_REQUEST, "Expire seconds must be greater than 0");
            }
            return new PresignedUrlRequest(this);
        }
    }
}