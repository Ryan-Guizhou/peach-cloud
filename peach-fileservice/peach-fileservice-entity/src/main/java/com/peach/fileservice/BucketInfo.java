package com.peach.fileservice;

/**
 * 存储桶信息。
 * <p>封装存储桶的基本信息，包括桶名称和对象键前缀。
 * 用于云存储服务的桶配置管理。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/19
 */
public class BucketInfo {

    /**
     * 存储桶名称
     */
    private final String bucketName;

    /**
     * 对象键前缀（用于隔离不同业务的文件）
     */
    private final String prefix;

    /**
     * 构造方法
     *
     * @param bucketName 存储桶名称
     * @param prefix 对象键前缀
     */
    public BucketInfo(String bucketName, String prefix) {
        this.bucketName = bucketName;
        this.prefix = prefix;
    }

    /**
     * 获取存储桶名称
     *
     * @return 存储桶名称
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * 获取对象键前缀
     *
     * @return 对象键前缀
     */
    public String getPrefix() {
        return prefix;
    }
}
