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
@Table(name = "VALUE_SET")
@Schema(description = "值集主表实体")
public class ValueSetDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ID")
    @Schema(description = "主键ID")
    private Long id;

    @Column(name = "CODE")
    @Schema(description = "值集编码")
    private String code;

    @Column(name = "NAME")
    @Schema(description = "值集名称")
    private String name;

    @Column(name = "DESCRIPTION")
    @Schema(description = "值集描述")
    private String description;

    @Column(name = "STATUS")
    @Schema(description = "状态：1-启用 0-停用")
    private Integer status;

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
        System.out.println(MapperGenerator.genMapper(ValueSetDO.class));
    }
}