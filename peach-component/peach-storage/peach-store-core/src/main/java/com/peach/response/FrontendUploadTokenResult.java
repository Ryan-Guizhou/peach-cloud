package com.peach.response;

import java.time.Instant;

/**
 * 前端直传令牌结果。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/18 14:15
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

    public FrontendUploadTokenResult(String providerName, String bucketName, String objectKey, String host,
                                     String accessKeyId, String policy, String signature, Instant expiresAt) {
        this.providerName = providerName;
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.host = host;
        this.accessKeyId = accessKeyId;
        this.policy = policy;
        this.signature = signature;
        this.expiresAt = expiresAt;
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
        return policy; }


    public String getSignature() {
        return signature;
    }


    public Instant getExpiresAt() {
        return expiresAt;
    }
}