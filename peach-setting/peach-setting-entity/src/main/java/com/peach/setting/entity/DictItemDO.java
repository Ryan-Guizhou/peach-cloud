package com.peach.setting.entity;

import com.peach.common.BaseDO;
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
 * @Description 字典项
 */
@Data
@Entity
@Table(name = "PEACH_DICT_ITEM")
@Schema(description = "字典项")
@EqualsAndHashCode(callSuper = true)
public class DictItemDO extends BaseDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ID")
    @Schema(description = "主键ID")
    private String id;

    @Column(name = "DICT_CODE")
    @Schema(description = "字典编码")
    private String dictCode;

    @Column(name = "ITEM_CODE")
    @Schema(description = "项编码")
    private String itemCode;

    @Column(name = "ITEM_VALUE")
    @Schema(description = "项值")
    private String itemValue;

    @Column(name = "SORT_ORDER")
    @Schema(description = "排序号")
    private Integer sortOrder;

    @Column(name = "MESSAGE_KEY")
    @Schema(description = "国际化Key")
    private String messageKey;

    @Column(name = "EXTRA_JSON")
    @Schema(description = "扩展JSON")
    private String extraJson;

    @Column(name = "STATUS")
    @Schema(description = "状态")
    private Integer status;
}
