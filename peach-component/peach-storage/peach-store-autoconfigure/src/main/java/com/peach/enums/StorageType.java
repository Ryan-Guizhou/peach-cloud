package com.peach.enums;

/**
 * 存储类型枚举。
 * <p>枚举只表达存储产品或协议类型，不绑定具体 SDK 实现。具体实现由
 * {@code StorageProviderFactory} 通过 SPI 按类型创建，业务侧可以在不修改
 * starter 源码的情况下扩展新的存储类型。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/10 14:49
 */
public enum StorageType {

    /**
     * 阿里云对象存储 OSS，推荐使用 aliyun-oss-sdk。
     */
    OSS,

    /**
     * 华为云对象存储 OBS，推荐使用 huaweicloud obs SDK。
     */
    OBS,

    /**
     * 百度云对象存储 BOS，推荐使用 baidubce SDK。
     */
    BOS,

    /**
     * 腾讯云对象存储 COS，推荐使用 qcloud COS SDK。
     */
    COS,

    /**
     * NAS 或共享文件系统，按本地文件系统语义处理路径。
     */
    NAS,

    /**
     * SFTP。。
     */
    SFTP,

    /**
     * MinIO，对外兼容 S3 协议，也可以由独立 MinIO SDK 实现。
     */
    MINIO,

    /**
     * Ceph RGW，对外通常兼容 S3 协议，也可以由 Ceph/RadosGW 适配器实现。
     */
    CEPH,

    /**
     * AWS S3，推荐使用 AWS SDK for Java。
     */
    S3,

    /**
     * 本机磁盘存储，兼容 Windows 与 Linux 路径。
     */
    LOCAL;


    /**
     * 根据配置文本解析存储类型，忽略大小写并兼容常见别名。
     *
     * @param value 配置值，例如 oss、aws、amazon-s3
     * @return 存储类型
     * @throws IllegalArgumentException 当配置为空或无法识别时抛出
     */
    public static StorageType parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Storage type must not be blank");
        }
        String normalized = value.trim().replace('-', '_').toUpperCase();
        if ("AWS".equals(normalized) || "AWS_S3".equals(normalized) || "AMAZON_S3".equals(normalized)
                || "AMAZONAWS".equals(normalized) || "AWAZONAWS".equals(normalized)) {
            return S3;
        }
        if ("FILE".equals(normalized) || "FILESYSTEM".equals(normalized) || "LOCAL_FILE".equals(normalized)) {
            return LOCAL;
        }
        return StorageType.valueOf(normalized);
    }
}
