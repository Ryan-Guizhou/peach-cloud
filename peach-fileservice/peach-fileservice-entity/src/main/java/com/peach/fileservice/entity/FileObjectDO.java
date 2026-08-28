package com.peach.fileservice.entity;

import java.io.Serial;

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
 * 文件物理对象实体。
 * <p>映射表 {@code PEACH_FILE_OBJECT}，表示文件在对象存储中的物理实体。
 * 记录文件的 SHA-256/MD5 摘要、大小、存储提供方与桶信息、对象Key、
 * 原始文件名、MIME类型、扩展名等元数据，并通过引用计数（refCount）
 * 支持多对一的文件去重与秒传场景。同时维护存储状态、上传时间、
 * 最后访问时间以及逻辑删除标记，用于文件生命周期管理。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/19
 */
@Data
@Entity
@Table(name = "PEACH_FILE_OBJECT")
@Schema(description = "文件物理对象实体")
@EqualsAndHashCode(callSuper = true)
public class FileObjectDO extends PeachDO implements Serializable {

    @Serial
    private static final long serialVersionUID = -4923191473705995553L;

    @Id
    @Column(name = "OBJECT_ID")
    @Schema(description = "物理对象ID")
    private String objectId;

    @Column(name = "HASH_SHA256")
    @Schema(description = "SHA-256摘要")
    private String hashSha256;

    @Column(name = "TENANT_ID")
    @Schema(description = "租户ID")
    private String tenantId;

    @Column(name = "ORG_ID")
    @Schema(description = "机构ID")
    private String orgId;

    @Column(name = "HASH_MD5")
    @Schema(description = "MD5摘要")
    private String hashMd5;

    @Column(name = "FILE_SIZE")
    @Schema(description = "文件大小")
    private Long fileSize;

    @Column(name = "STORAGE_PROVIDER")
    @Schema(description = "存储提供方")
    private String storageProvider;

    @Column(name = "BUCKET_NAME")
    @Schema(description = "bucket名称")
    private String bucketName;

    @Column(name = "OBJECT_KEY")
    @Schema(description = "对象key")
    private String objectKey;

    @Column(name = "ORIGIN_FILE_NAME")
    @Schema(description = "原始文件名")
    private String originFileName;

    @Column(name = "CONTENT_TYPE")
    @Schema(description = "内容类型")
    private String contentType;

    @Column(name = "EXTENSION")
    @Schema(description = "扩展名")
    private String extension;

    @Column(name = "STORAGE_STATUS")
    @Schema(description = "存储状态")
    private String storageStatus;

    @Column(name = "REF_COUNT")
    @Schema(description = "引用数量")
    private Integer refCount;

    @Column(name = "UPLOAD_TIME")
    @Schema(description = "上传完成时间")
    private String uploadTime;

    @Column(name = "LAST_ACCESS_TIME")
    @Schema(description = "最后访问时间")
    private String lastAccessTime;

    @Column(name = "IS_DELETE")
    @Schema(description = "逻辑删除标记")
    private Integer isDelete;
}
