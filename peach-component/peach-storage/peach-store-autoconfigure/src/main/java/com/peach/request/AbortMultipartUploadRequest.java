package com.peach.request;

import com.peach.enums.StorageResultCode;
import com.peach.exception.StorageException;

/**
 * 中止分片上传请求。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/18 14:15
 */
public class AbortMultipartUploadRequest extends StorageObjectRequest {

    /**
     * 分片上传会话标识。
     */
    private final String uploadId;

    private AbortMultipartUploadRequest(Builder builder) {
        super(builder);
        this.uploadId = builder.uploadId;
    }

    /**
     * 创建中止分片上传请求构造器。
     *
     * @return 构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 获取分片上传会话标识。
     *
     * @return 分片上传会话标识
     */
    public String getUploadId() {
        return uploadId;
    }

    public static class Builder extends StorageObjectRequest.Builder<Builder> {

        private String uploadId;

        public Builder uploadId(String uploadId) {
            this.uploadId = uploadId;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        public AbortMultipartUploadRequest build() {
            validate();
            if (uploadId == null || uploadId.isBlank()) {
                throw new StorageException(StorageResultCode.BAD_REQUEST, "Upload id must not be blank");
            }
            return new AbortMultipartUploadRequest(this);
        }
    }
}