package com.peach.setting.qo;

import com.peach.common.PeachEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:40
 * @Description 值集项查询对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "值集项查询对象")
public class ValueSetItemQO extends PeachEntity implements Serializable {

    private static final long serialVersionUID = 1L;

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
