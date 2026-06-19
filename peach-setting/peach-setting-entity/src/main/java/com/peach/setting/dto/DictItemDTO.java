package com.peach.setting.dto;

import com.peach.setting.comon.enums.DictItemGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 21:50
 * @Description 字典项DTO
 */
@Data
public class DictItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "字典项ID")
    @NotBlank(groups = {DictItemGroup.UpdatetGroup.class}, message = "字典项ID不能为空")
    private String id;

    @Schema(description = "字典编码")
    @NotBlank(groups = {DictItemGroup.InsertGroup.class}, message = "字典编码不能为空")
    private String dictCode;

    @Schema(description = "字典项编码")
    @NotBlank(groups = {DictItemGroup.InsertGroup.class}, message = "字典项编码不能为空")
    private String itemCode;

    @Schema(description = "字典项值")
    @NotBlank(groups = {DictItemGroup.InsertGroup.class}, message = "字典项值不能为空")
    private String itemValue;

    @Schema(description = "国际化消息键")
    private String messageKey;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "状态")
    private Integer status;
}
