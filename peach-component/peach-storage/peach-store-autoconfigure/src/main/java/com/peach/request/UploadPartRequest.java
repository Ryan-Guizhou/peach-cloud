package com.peach.request;

import com.peach.enums.StorageResultCode;
import com.peach.exception.StorageException;

/**
 * UploadPartRequest相关类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/18 14:15
 */
public class UploadPartRequest extends StorageObjectRequest {

    /**
     * 分片上传会话标识。
     */
    private final String uploadId;

    /**
     * 分片序号，从 1 开始。
     */
    private final int partNumber;

    /**
     * 分片上传地址有效期，单位秒。
     */
    private final long expireSeconds;

    private UploadPartRequest(Builder builder) {
        super(builder);
        this.uploadId = builder.uploadId;
        this.partNumber = builder.partNumber;
        this.expireSeconds = builder.expireSeconds;
    }


    public static Builder builder() {
        return new Builder();
    }


    public String getUploadId() {
        return uploadId;
    }


    public int getPartNumber() {
        return partNumber;
    }


    public long getExpireSeconds() {
        return expireSeconds;
    }

    public static class Builder extends StorageObjectRequest.Builder<Builder> {

        private String uploadId;
        private int partNumber;
        private long expireSeconds = 900L;

        public Builder uploadId(String uploadId) {
            this.uploadId = uploadId;
            return this;
        }

        public Builder partNumber(int partNumber) {
            this.partNumber = partNumber;
            return this;
        }

        public Builder expireSeconds(long expireSeconds) {
            this.expireSeconds = expireSeconds;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public UploadPartRequest build() {
            validate();
            if (uploadId == null || uploadId.isBlank()) {
                throw new StorageException(StorageResultCode.BAD_REQUEST, "Upload id must not be blank");
            }
            if (partNumber <= 0) {
                throw new StorageException(StorageResultCode.BAD_REQUEST, "Part number must be greater than 0");
            }
            if (expireSeconds <= 0) {
                throw new StorageException(StorageResultCode.BAD_REQUEST, "Expire seconds must be greater than 0");
            }
            return new UploadPartRequest(this);
        }
    }
}
