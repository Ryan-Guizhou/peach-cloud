package com.peach.setting.dto;

import com.peach.setting.comon.enums.DictTypeGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 21:57
 * @Description 字典类型DTO
 */
@Data
public class DictTypeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "字典类型ID")
    @NotBlank(groups = {DictTypeGroup.UpdatetGroup.class}, message = "字典类型ID不能为空")
    private String id;

    @Schema(description = "字典编码")
    @NotBlank(groups = {DictTypeGroup.InsertGroup.class}, message = "参数不能为空")
    private String dictCode;

    @Schema(description = "字典类型名称")
    @NotBlank(groups = {DictTypeGroup.InsertGroup.class}, message = "字典类型名称不能为空")
    private String dictName;

    @Schema(description = "模块编码")
    private String moduleCode;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "状态")
    private Integer status;
}
