package com.peach.response;


/**
 * 上传结果。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/16 14:01
 */
public class UploadResult {

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
     * 对象大小，单位字节。
     */
    private final long size;

    /**
     * 对象访问地址。
     */
    private final String url;

    /**
     * 对象 ETag。
     */
    private final String etag;

    public UploadResult(String providerName, String bucketName, String objectKey, long size, String url, String etag) {
        this.providerName = providerName;
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.size = size;
        this.url = url;
        this.etag = etag;
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

    public long getSize() {
        return size;
    }

    public String getUrl() {
        return url;
    }

    public String getEtag() {
        return etag;
    }
}
