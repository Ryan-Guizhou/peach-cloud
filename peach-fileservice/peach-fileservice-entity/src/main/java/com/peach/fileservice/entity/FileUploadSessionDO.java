package com.peach.fileservice.entity;

import com.peach.common.PeachDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * 文件上传会话数据库实体
 *
 * <p>映射表 {@code PEACH_FILE_UPLOAD_SESSION}，用于持久化文件上传过程中的会话信息。
 * 每次文件上传操作创建一条会话记录，贯穿上传全生命周期，主要包含：</p>
 * <ul>
 *   <li>会话标识：会话ID、预生成文件ID、预生成物理对象ID</li>
 *   <li>文件摘要：SHA-256、MD5，用于完整性校验与秒传判定</li>
 *   <li>文件元数据：文件名、显示名、大小、内容类型</li>
 *   <li>业务关联：业务类型、业务ID、业务标签、备注</li>
 *   <li>存储信息：存储提供方、Bucket名称、对象Key、底层上传会话ID</li>
 *   <li>生命周期：会话状态、过期时间、逻辑删除标记</li>
 * </ul>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/19
 */
@Data
@Entity
@Table(name = "PEACH_FILE_UPLOAD_SESSION")
@Schema(description = "文件上传会话实体")
@EqualsAndHashCode(callSuper = true)
public class FileUploadSessionDO extends PeachDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "SESSION_ID")
    @Schema(description = "上传会话ID")
    private String sessionId;

    @Column(name = "FILE_ID")
    @Schema(description = "预生成文件ID")
    private String fileId;

    @Column(name = "OBJECT_ID")
    @Schema(description = "预生成物理对象ID")
    private String objectId;

    @Column(name = "TENANT_ID")
    @Schema(description = "租户ID")
    private String tenantId;

    @Column(name = "ORG_ID")
    @Schema(description = "机构ID")
    private String orgId;

    @Column(name = "HASH_SHA256")
    @Schema(description = "SHA-256摘要")
    private String hashSha256;

    @Column(name = "HASH_MD5")
    @Schema(description = "MD5摘要")
    private String hashMd5;

    @Column(name = "FILE_SIZE")
    @Schema(description = "文件大小")
    private Long fileSize;

    @Column(name = "FILE_NAME")
    @Schema(description = "文件名")
    private String fileName;

    @Column(name = "DISPLAY_NAME")
    @Schema(description = "显示文件名")
    private String displayName;

    @Column(name = "CONTENT_TYPE")
    @Schema(description = "内容类型")
    private String contentType;

    @Column(name = "BIZ_TYPE")
    @Schema(description = "业务类型")
    private String bizType;

    @Column(name = "BIZ_ID")
    @Schema(description = "业务ID")
    private String bizId;

    @Column(name = "BIZ_TAG")
    @Schema(description = "业务标签")
    private String bizTag;

    @Column(name = "REMARK")
    @Schema(description = "备注")
    private String remark;

    @Column(name = "STORAGE_PROVIDER")
    @Schema(description = "存储提供方")
    private String storageProvider;

    @Column(name = "BUCKET_NAME")
    @Schema(description = "bucket名称")
    private String bucketName;

    @Column(name = "OBJECT_KEY")
    @Schema(description = "对象key")
    private String objectKey;

    @Column(name = "UPLOAD_ID")
    @Schema(description = "底层上传会话ID")
    private String uploadId;

    @Column(name = "SESSION_STATUS")
    @Schema(description = "会话状态")
    private String sessionStatus;

    @Column(name = "EXPIRE_TIME")
    @Schema(description = "过期时间")
    private String expireTime;

    @Column(name = "IS_DELETE")
    @Schema(description = "逻辑删除标记")
    private Integer isDelete;
}
