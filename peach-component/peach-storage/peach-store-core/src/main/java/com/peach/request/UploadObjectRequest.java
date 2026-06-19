package com.peach.request;

import com.peach.content.UploadContent;
import com.peach.enums.StorageContentType;
import com.peach.enums.StorageResultCode;
import com.peach.exception.StorageException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 上传对象请求。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/16 14:01
 */
public class UploadObjectRequest extends StorageObjectRequest {

    /**
     * 上传内容读取器。
     */
    private final UploadContent content;

    /**
     * 对象内容类型。
     */
    private final String contentType;

    /**
     * 自定义元数据。
     */
    private final Map<String, String> metadata;

    /**
     * 上传完成后是否设置为公共读。
     */
    private final boolean publicRead;

    private UploadObjectRequest(Builder builder) {
        super(builder);
        this.content = builder.content;
        this.contentType = builder.contentType;
        this.metadata = new LinkedHashMap<>(builder.metadata);
        this.publicRead = builder.publicRead;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UploadContent getContent() {
        return content;
    }

    public String getContentType() {
        return contentType;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public boolean isPublicRead() {
        return publicRead;
    }

    public static class Builder extends StorageObjectRequest.Builder<Builder> {

        private UploadContent content;
        private String contentType;
        private Map<String, String> metadata = new LinkedHashMap<>();
        private boolean publicRead;

        public Builder content(UploadContent content) {
            this.content = content;
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder contentType(StorageContentType contentType) {
            this.contentType = contentType == null ? null : contentType.value();
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
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

        public UploadObjectRequest build() {
            validate();
            if (content == null) {
                throw new StorageException(StorageResultCode.BAD_REQUEST, "Upload content must not be null");
            }
            return new UploadObjectRequest(this);
        }
    }
}
