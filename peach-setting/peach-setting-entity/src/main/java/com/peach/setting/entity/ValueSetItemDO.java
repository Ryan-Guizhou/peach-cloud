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
 * 值集项。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:30
 * @Description 值集项
 */
@Data
@Entity
@Table(name = "PEACH_VALUE_SET_ITEM")
@Schema(description = "值集项")
@EqualsAndHashCode(callSuper = true)
public class ValueSetItemDO extends PeachDO implements Serializable {

    @Serial
    private static final long serialVersionUID = -384043292077429178L;

    @Id
    @Column(name = "ID")
    @Schema(description = "主键ID")
    private String id;

    @Column(name = "VALUE_SET_CODE")
    @Schema(description = "值集编码")
    private String valueSetCode;

    @Column(name = "TENANT_ID")
    @Schema(description = "租户ID")
    private String tenantId;

    @Column(name = "ORG_ID")
    @Schema(description = "机构ID")
    private String orgId;

    @Column(name = "ITEM_CODE")
    @Schema(description = "项编码")
    private String itemCode;

    @Column(name = "ITEM_VALUE")
    @Schema(description = "项值")
    private String itemValue;

    @Column(name = "MESSAGE_KEY")
    @Schema(description = "国际化Key")
    private String messageKey;

    @Column(name = "SOURCE_TYPE")
    @Schema(description = "来源类型")
    private String sourceType;

    @Column(name = "SORT_ORDER")
    @Schema(description = "排序号")
    private Integer sortOrder;

    @Column(name = "STATUS")
    @Schema(description = "状态")
    private Integer status;

    @Column(name = "VISIBLE_FLAG")
    @Schema(description = "是否可见")
    private Integer visibleFlag;

    @Column(name = "EXTRA_JSON")
    @Schema(description = "扩展JSON")
    private String extraJson;
}
