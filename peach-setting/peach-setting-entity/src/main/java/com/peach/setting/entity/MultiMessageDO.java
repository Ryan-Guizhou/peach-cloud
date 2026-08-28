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
 * 多语言消息。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:30
 */

@Data
@Entity
@Table(name = "PEACH_MULTI_MESSAGE")
@Schema(description = "多语言消息")
@EqualsAndHashCode(callSuper = true)
public class MultiMessageDO extends PeachDO implements Serializable {

    @Serial
    private static final long serialVersionUID = -2673161574061145964L;

    @Id
    @Column(name = "ID")
    @Schema(description = "主键ID")
    private String id;

    @Column(name = "MESSAGE_KEY")
    @Schema(description = "消息键")
    private String messageKey;

    @Column(name = "LOCALE")
    @Schema(description = "语言区域")
    private String locale;

    @Column(name = "TENANT_ID")
    @Schema(description = "租户ID")
    private String tenantId;

    @Column(name = "ORG_ID")
    @Schema(description = "机构ID")
    private String orgId;

    @Column(name = "MODULE_CODE")
    @Schema(description = "模块编码")
    private String moduleCode;

    @Column(name = "MESSAGE_TYPE")
    @Schema(description = "消息类型")
    private String messageType;

    @Column(name = "USAGE_SCOPE")
    @Schema(description = "使用范围：COMMON通用、BACKEND后端、FRONTEND前端、BOTH全部")
    private String usageScope;

    @Column(name = "MESSAGE_CONTENT")
    @Schema(description = "消息内容")
    private String messageContent;

    @Column(name = "DESCRIPTION")
    @Schema(description = "描述")
    private String description;

    @Column(name = "STATUS")
    @Schema(description = "状态")
    private Integer status;
}

