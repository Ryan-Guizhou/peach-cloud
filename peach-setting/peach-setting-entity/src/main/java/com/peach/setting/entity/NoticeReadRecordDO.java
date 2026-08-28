package com.peach.setting.entity;

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
 * 通知阅读记录。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:30
 * @Description 通知阅读记录
 */
@Data
@Entity
@Table(name = "PEACH_NOTICE_READ_RECORD")
@Schema(description = "通知阅读记录")
@EqualsAndHashCode(callSuper = true)
public class NoticeReadRecordDO extends PeachDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1239394034196763740L;

    @Id
    @Column(name = "ID")
    @Schema(description = "主键ID")
    private String id;

    @Column(name = "NOTICE_CODE")
    @Schema(description = "通知编码")
    private String noticeCode;

    @Column(name = "TENANT_ID")
    @Schema(description = "租户ID")
    private String tenantId;

    @Column(name = "ORG_ID")
    @Schema(description = "机构ID")
    private String orgId;

    @Column(name = "READ_USER_ID")
    @Schema(description = "阅读用户ID")
    private String readUserId;

    @Column(name = "READ_TIME")
    @Schema(description = "阅读时间")
    private String readTime;
}
