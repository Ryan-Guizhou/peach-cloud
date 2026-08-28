package com.peach.fileservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件Domain配置属性。
 * <p>定义文件服务的核心配置参数，包括存储提供者、对象键前缀、
 * 保留期、URL过期时间、清理任务等配置项。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/19
 */
@Data
@Component("fileDomainProperties")
@ConfigurationProperties(prefix = "peach.file")
public class FileDomainProperties {

    /**
     * 默认存储提供者名称
     * <p>用于指定默认使用的云存储服务（如 minio、oss、cos 等）</p>
     */
    private String defaultProvider;

    /**
     * 对象键前缀
     * <p>所有上传文件的对象键都会添加此前缀，用于隔离不同业务的文件</p>
     */
    private String objectKeyPrefix = "files";

    /**
     * 逻辑删除文件保留天数
     * <p>文件逻辑删除后，在保留期内可恢复，超过后物理删除</p>
     */
    private Integer retentionDays = 30;

    /**
     * 下载URL过期时间（秒）
     * <p>预签名下载URL的有效期</p>
     */
    private Long downloadUrlExpireSeconds = 3600L;

    /**
     * 分片上传URL过期时间（秒）
     * <p>分片预签名上传URL的有效期</p>
     */
    private Long partUrlExpireSeconds = 900L;

    /**
     * 上传会话过期时间（分钟）
     * <p>分片上传会话的超时时间，超时后自动清理</p>
     */
    private Integer uploadSessionExpireMinutes = 120;

    /**
     * 是否启用过期删除文件清理任务
     */
    private Boolean cleanupEnabled = Boolean.TRUE;

    /**
     * 过期删除文件清理任务的 Cron 表达式
     * <p>默认每天凌晨3点执行</p>
     */
    private String cleanupCron = "0 0 3 * * ?";

    /**
     * 是否启用过期上传会话清理任务
     */
    private Boolean uploadSessionCleanupEnabled = Boolean.TRUE;

    /**
     * 过期上传会话清理任务的 Cron 表达式
     * <p>默认每30分钟执行一次</p>
     */
    private String uploadSessionCleanupCron = "0 0/30 * * * ?";
}
