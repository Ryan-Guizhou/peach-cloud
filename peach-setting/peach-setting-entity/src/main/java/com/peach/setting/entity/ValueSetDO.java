package com.peach.setting.entity;

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
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:30
 * @Description 值集配置
 */
@Data
@Entity
@Table(name = "PEACH_VALUE_SET")
@Schema(description = "值集配置")
@EqualsAndHashCode(callSuper = true)
public class ValueSetDO extends PeachDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ID")
    @Schema(description = "主键ID")
    private String id;

    @Column(name = "VALUE_SET_CODE")
    @Schema(description = "值集编码")
    private String valueSetCode;

    @Column(name = "VALUE_SET_NAME")
    @Schema(description = "值集名称")
    private String valueSetName;

    @Column(name = "MODULE_CODE")
    @Schema(description = "模块编码")
    private String moduleCode;

    @Column(name = "SOURCE_TYPE")
    @Schema(description = "来源类型")
    private String sourceType;

    @Column(name = "DESCRIPTION")
    @Schema(description = "描述")
    private String description;

    @Column(name = "STATUS")
    @Schema(description = "状态")
    private Integer status;



}

