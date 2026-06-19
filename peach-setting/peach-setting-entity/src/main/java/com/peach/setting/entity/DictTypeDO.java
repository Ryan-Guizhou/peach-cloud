package com.peach.setting.entity;

import com.peach.common.BaseDO;
import com.peach.common.MapperGenerator;
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
 * @Description 字典类型
 */
@Data
@Entity
@Table(name = "PEACH_DICT_TYPE")
@Schema(description = "字典类型")
@EqualsAndHashCode(callSuper = true)
public class DictTypeDO extends BaseDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ID")
    @Schema(description = "主键ID")
    private String id;

    @Column(name = "DICT_CODE")
    @Schema(description = "字典编码")
    private String dictCode;

    @Column(name = "DICT_NAME")
    @Schema(description = "字典名称")
    private String dictName;

    @Column(name = "MODULE_CODE")
    @Schema(description = "模块编码")
    private String moduleCode;

    @Column(name = "SORT_ORDER")
    @Schema(description = "排序号")
    private Integer sortOrder;

    @Column(name = "STATUS")
    @Schema(description = "状态")
    private Integer status;
}
