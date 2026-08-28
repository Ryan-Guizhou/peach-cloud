package com.peach.response;

import java.time.Instant;

/**
 * Frontend上传令牌结果。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/18 14:15
 */
public class FrontendUploadTokenResult {

    /**
     * 存储提供者名称。
     */
    private final String providerName;

    /**
     * 存储桶名称。
     */
    private final String bucketName;

    /**
     * 对象 key。
     */
    private final String objectKey;

    /**
     * 前端直传地址。
     */
    private final String host;

    /**
     * 临时访问标识。
     */
    private final String accessKeyId;

    /**
     * 前端直传策略内容。
     */
    private final String policy;

    /**
     * 签名串。
     */
    private final String signature;

    /**
     * 过期时间。
     */
    private final Instant expiresAt;

    private FrontendUploadTokenResult(Builder builder) {
        this.providerName = builder.providerName;
        this.bucketName = builder.bucketName;
        this.objectKey = builder.objectKey;
        this.host = builder.host;
        this.accessKeyId = builder.accessKeyId;
        this.policy = builder.policy;
        this.signature = builder.signature;
        this.expiresAt = builder.expiresAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getProviderName() {
        return providerName;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getHost() {
        return host;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public String getPolicy() {
        return policy;
    }

    public String getSignature() {
        return signature;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    /**
     * 前端直传令牌构建器。
     */
    public static final class Builder {
        private String providerName;
        private String bucketName;
        private String objectKey;
        private String host;
        private String accessKeyId;
        private String policy;
        private String signature;
        private Instant expiresAt;

        public Builder providerName(String providerName) {
            this.providerName = providerName;
            return this;
        }

        public Builder bucketName(String bucketName) {
            this.bucketName = bucketName;
            return this;
        }

        public Builder objectKey(String objectKey) {
            this.objectKey = objectKey;
            return this;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder accessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
            return this;
        }

        public Builder policy(String policy) {
            this.policy = policy;
            return this;
        }

        public Builder signature(String signature) {
            this.signature = signature;
            return this;
        }

        public Builder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public FrontendUploadTokenResult build() {
            return new FrontendUploadTokenResult(this);
        }
    }
}
