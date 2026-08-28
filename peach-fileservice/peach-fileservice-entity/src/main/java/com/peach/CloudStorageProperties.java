package com.peach;

import lombok.Data;

/**
 * 云存储配置属性。
 * <p>定义云存储服务的连接配置，包括访问凭证、桶信息、端点等。
 * 支持多种云存储服务（如 MinIO、OSS、COS、OBS 等）。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/19
 */
@Data
public class CloudStorageProperties {

    /**
     * 访问密钥 ID
     */
    private String accessKey;

    /**
     * 访问密钥 Secret
     */
    private String secretKey;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 服务端点 URL
     */
    private String endpoint;

    /**
     * 区域标识（部分云存储服务需要）
     */
    private String region;
}
