package com.peach.setting.dto;

import java.io.Serial;

import com.peach.setting.comon.enums.ValueSetGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 值集DTO。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 21:50
 * @Description 值集DTO
 */
@Data
public class ValueSetDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -2509469467596655810L;

    @Schema(description = "主键ID")
    @NotBlank(groups = {ValueSetGroup.UpdatetGroup.class}, message = "主键ID不能为空")
    private String id;

    @Schema(description = "值集编码")
    @NotBlank(groups = {ValueSetGroup.InsertGroup.class}, message = "值集编码不能为空")
    private String valueSetCode;

    @Schema(description = "值集名称")
    @NotBlank(groups = {ValueSetGroup.InsertGroup.class}, message = "值集名称不能为空")
    private String valueSetName;

    @Schema(description = "模块编码")
    private String moduleCode;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "状态")
    private Integer status;
}
