package com.peach.setting.entity;

import com.peach.common.MapperGenerator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * @Author Trae AI
 * @Version 1.0.0
 * @CreateTime 2026/02/09
 */
@Data
@Entity
@Table(name = "LANGUAGE")
@Schema(description = "系统语言配置实体")
public class LanguageDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ID")
    @Schema(description = "主键ID")
    private Long id;

    @Column(name = "CODE")
    @Schema(description = "语言编码")
    private String code;

    @Column(name = "NAME")
    @Schema(description = "显示名称")
    private String name;

    @Column(name = "ICON")
    @Schema(description = "语言图标")
    private String icon;

    @Column(name = "IS_DEFAULT")
    @Schema(description = "是否默认语言")
    private Integer isDefault;

    @Column(name = "STATUS")
    @Schema(description = "状态：1-启用 0-停用")
    private Integer status;

    @Column(name = "SORT_ORDER")
    @Schema(description = "排序值")
    private Integer sortOrder;


    @Column(name = "CREATOR_CODE")
    @Schema(description = "创建人编码")
    private String creatorCode;

    @Column(name = "CREATOR_NAME")
    @Schema(description = "创建人名称")
    private String creatorName;

    @Column(name = "CREATED_TIME")
    @Schema(description = "创建时间")
    private String createdTime;

    @Column(name = "UPDATED_TIME")
    @Schema(description = "更新时间")
    private String updatedTime;

    @Column(name = "UPDATER_CODE")
    @Schema(description = "更新人编码")
    private String updaterCode;

    @Column(name = "UPDATER_NAME")
    @Schema(description = "更新人名称")
    private String updaterName;

    public static void main(String[] args) {
        System.out.println(MapperGenerator.genMapper(LanguageDO.class));
    }
}