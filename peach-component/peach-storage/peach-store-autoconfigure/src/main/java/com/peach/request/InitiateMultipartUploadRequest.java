package com.peach.request;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * InitiateMultipartUploadRequest相关类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/18 14:15
 */
public class InitiateMultipartUploadRequest extends StorageObjectRequest {

    /**
     * 上传对象的内容类型。
     */
    private final String contentType;

    /**
     * 上传对象的自定义元数据。
     */
    private final Map<String, String> metadata;

    /**
     * 是否在上传完成后设置为公共读。
     */
    private final boolean publicRead;

    private InitiateMultipartUploadRequest(Builder builder) {
        super(builder);
        this.contentType = builder.contentType;
        this.metadata = new LinkedHashMap<>(builder.metadata);
        this.publicRead = builder.publicRead;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 获取上传对象的内容类型。
     *
     * @return 内容类型
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * 获取上传对象的自定义元数据。
     *
     * @return 自定义元数据
     */
    public Map<String, String> getMetadata() {
        return metadata;
    }

    /**
     * 判断是否在上传完成后设置为公共读。
     *
     * @return true 表示设置为公共读
     */
    public boolean isPublicRead() {
        return publicRead;
    }

    public static class Builder extends StorageObjectRequest.Builder<Builder> {

        private String contentType;
        private Map<String, String> metadata = new LinkedHashMap<>();
        private boolean publicRead;

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata == null ? new LinkedHashMap<String, String>() : new LinkedHashMap<>(metadata);
            return this;
        }

        public Builder publicRead(boolean publicRead) {
            this.publicRead = publicRead;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public InitiateMultipartUploadRequest build() {
            validate();
            return new InitiateMultipartUploadRequest(this);
        }
    }
}
