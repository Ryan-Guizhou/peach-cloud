package com.peach.setting.dto;

import java.io.Serial;

import com.peach.setting.comon.enums.ValueSetItemGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 值集项DTO。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 21:50
 * @Description 值集项DTO
 */
@Data
public class ValueSetItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 4465070466254386799L;

    @Schema(description = "主键ID")
    @NotBlank(groups = {ValueSetItemGroup.UpdatetGroup.class}, message = "主键ID不能为空")
    private String id;

    @Schema(description = "值集编码")
    @NotBlank(groups = {ValueSetItemGroup.InsertGroup.class}, message = "值集编码不能为空")
    private String valueSetCode;

    @Schema(description = "项编码")
    @NotBlank(groups = {ValueSetItemGroup.InsertGroup.class}, message = "项编码不能为空")
    private String itemCode;

    @Schema(description = "项值")
    @NotBlank(groups = {ValueSetItemGroup.InsertGroup.class}, message = "项值不能为空")
    private String itemValue;

    @Schema(description = "国际化消息Key")
    private String messageKey;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "可见标志")
    private Integer visibleFlag;

    @Schema(description = "扩展JSON")
    private String extraJson;
}
