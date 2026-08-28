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
 * 语言设置。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:30
 * @Description 语言设置实体
 */
@Data
@Entity
@Table(name = "PEACH_LANGUAGE")
@Schema(description = "语言设置")
@EqualsAndHashCode(callSuper = true)
public class LanguageDO extends PeachDO implements Serializable {

    @Serial
    private static final long serialVersionUID = -7803072824918809031L;

    @Id
    @Column(name = "ID")
    @Schema(description = "语言ID")
    private String id;

    @Column(name = "LANGUAGE_CODE")
    @Schema(description = "语言编码，如zh_CN或en_US")
    private String languageCode;

    @Column(name = "TENANT_ID")
    @Schema(description = "租户ID")
    private String tenantId;

    @Column(name = "ORG_ID")
    @Schema(description = "机构ID")
    private String orgId;

    @Column(name = "LANGUAGE_NAME")
    @Schema(description = "语言名称")
    private String languageName;

    @Column(name = "NATIVE_NAME")
    @Schema(description = "本地化名称")
    private String nativeName;

    @Column(name = "ICON")
    @Schema(description = "语言图标")
    private String icon;

    @Column(name = "STATUS")
    @Schema(description = "状态")
    private Integer status;

    @Column(name = "DEFAULT_FLAG")
    @Schema(description = "是否默认")
    private Integer defaultFlag;

    @Column(name = "SORT_ORDER")
    @Schema(description = "排序")
    private Integer sortOrder;
}
