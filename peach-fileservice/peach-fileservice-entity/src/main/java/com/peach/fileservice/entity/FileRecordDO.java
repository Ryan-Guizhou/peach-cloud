package com.peach.fileservice.entity;

import com.peach.common.PeachDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * 业务文件记录数据库实体
 *
 * <p>映射数据库表 PEACH_FILE_RECORD，用于持久化业务文件记录信息。
 * 记录文件与业务的关联关系，包含文件元数据（名称、类型、大小、扩展名）、
 * 业务归属标识（业务类型、业务ID、业务标签）以及逻辑删除与过期清理策略。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/19
 */
@Data
@Entity
@Table(name = "PEACH_FILE_RECORD")
@Schema(description = "业务文件记录实体")
@EqualsAndHashCode(callSuper = true)
public class FileRecordDO extends PeachDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "FILE_ID")
    @Schema(description = "业务文件ID")
    private String fileId;

    @Column(name = "OBJECT_ID")
    @Schema(description = "物理对象ID")
    private String objectId;

    @Column(name = "TENANT_ID")
    @Schema(description = "租户ID")
    private String tenantId;

    @Column(name = "ORG_ID")
    @Schema(description = "机构ID")
    private String orgId;

    @Column(name = "BIZ_TYPE")
    @Schema(description = "业务类型")
    private String bizType;

    @Column(name = "BIZ_ID")
    @Schema(description = "业务ID")
    private String bizId;

    @Column(name = "BIZ_TAG")
    @Schema(description = "业务标签")
    private String bizTag;

    @Column(name = "FILE_NAME")
    @Schema(description = "文件名")
    private String fileName;

    @Column(name = "DISPLAY_NAME")
    @Schema(description = "显示文件名")
    private String displayName;

    @Column(name = "CONTENT_TYPE")
    @Schema(description = "内容类型")
    private String contentType;

    @Column(name = "FILE_SIZE")
    @Schema(description = "文件大小")
    private Long fileSize;

    @Column(name = "FILE_EXT")
    @Schema(description = "扩展名")
    private String fileExt;

    @Column(name = "FILE_STATUS")
    @Schema(description = "文件状态")
    private String fileStatus;

    @Column(name = "DELETE_TIME")
    @Schema(description = "逻辑删除时间")
    private String deleteTime;

    @Column(name = "EXPIRE_DELETE_TIME")
    @Schema(description = "过期删除时间")
    private String expireDeleteTime;

    @Column(name = "REMARK")
    @Schema(description = "备注")
    private String remark;

    @Column(name = "IS_DELETE")
    @Schema(description = "逻辑删除标记")
    private Integer isDelete;
}
