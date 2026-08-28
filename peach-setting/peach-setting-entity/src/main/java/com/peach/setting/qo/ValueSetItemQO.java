package com.peach.setting.qo;

import java.io.Serial;

import com.peach.common.PeachEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 值集项查询对象。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:40
 * @Description 值集项查询对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "值集项查询对象")
public class ValueSetItemQO extends PeachEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = -6856549050428742407L;

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "机构ID")
    private String orgId;

    @Schema(description = "值集项ID")
    private String id;

    @Schema(description = "值集编码")
    private String valueSetCode;

    @Schema(description = "项编码")
    private String itemCode;

    @Schema(description = "项值")
    private String itemValue;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "可见标识")
    private Integer visibleFlag;
}
