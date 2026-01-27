package com.peach.fileservice.config.store;

import lombok.Data;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/27 11:30
 */
@Data
public class BaseProperties {

    /**
     * 访问密钥 / Access key
     */
    private String accessKey;

    /**
     * 密钥 / Secret key
     */
    private String secretKey;

    /**
     * 存储服务地址 / Storage service address
     */
    private String endpoint;

    /**
     * 存储桶名称 / Bucket name
     */
    private String bucketName;

    /**
     * 是否开启clamav 检测 / Whether to enable clamav detection
     */
    private boolean isEnableClamav;

    /**
     * 文件存储路径前缀 / The file storage path prefix
     */
    private String prefix;

    /**
     * 代理地址 / Proxy address
     */
    private String proxyHost;
}
