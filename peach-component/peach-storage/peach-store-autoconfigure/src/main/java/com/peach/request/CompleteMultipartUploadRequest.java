package com.peach.request;

import com.peach.enums.StorageResultCode;
import com.peach.exception.StorageException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 完成分片上传请求。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/18 14:15
 */
public class CompleteMultipartUploadRequest extends StorageObjectRequest {

    /**
     * 分片上传会话标识。
     */
    private final String uploadId;

    /**
     * 已上传分片的完成信息集合。
     */
    private final List<Part> parts;

    private CompleteMultipartUploadRequest(Builder builder) {
        super(builder);
        this.uploadId = builder.uploadId;
        this.parts = Collections.unmodifiableList(new ArrayList<>(builder.parts));
    }

    /**
     * 创建完成分片上传请求构造器。
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

    /**
     * 获取已上传分片的完成信息集合。
     *
     * @return 分片完成信息集合
     */
    public List<Part> getParts() {
        return parts;
    }

    public static class Builder extends StorageObjectRequest.Builder<Builder> {

        private String uploadId;
        private List<Part> parts = new ArrayList<>();

        public Builder uploadId(String uploadId) {
            this.uploadId = uploadId;
            return this;
        }

        public Builder parts(List<Part> parts) {
            this.parts = parts == null ? new ArrayList<Part>() : new ArrayList<>(parts);
            return this;
        }

        public Builder addPart(int partNumber, String eTag) {
            this.parts.add(new Part(partNumber, eTag));
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        public CompleteMultipartUploadRequest build() {
            validate();
            if (uploadId == null || uploadId.isBlank()) {
                throw new StorageException(StorageResultCode.BAD_REQUEST, "Upload id must not be blank");
            }
            if (parts == null || parts.isEmpty()) {
                throw new StorageException(StorageResultCode.BAD_REQUEST, "Parts must not be empty");
            }
            for (Part part : parts) {
                if (part == null || part.getPartNumber() <= 0 || part.getETag() == null || part.getETag().isBlank()) {
                    throw new StorageException(StorageResultCode.BAD_REQUEST, "Part info is invalid");
                }
            }
            return new CompleteMultipartUploadRequest(this);
        }
    }

    /**
     * 分片完成信息。
     */
    public static class Part {

        /**
         * 分片序号，从 1 开始。
         */
        private final int partNumber;

        /**
         * 分片上传完成后返回的 ETag。
         */
        private final String eTag;

        public Part(int partNumber, String eTag) {
            this.partNumber = partNumber;
            this.eTag = eTag;
        }

        /**
         * 获取分片序号。
         *
         * @return 分片序号，从 1 开始
         */
        public int getPartNumber() {
            return partNumber;
        }

        /**
         * 获取分片上传完成后返回的 ETag。
         *
         * @return 分片 ETag
         */
        public String getETag() {
            return eTag;
        }
    }
}
