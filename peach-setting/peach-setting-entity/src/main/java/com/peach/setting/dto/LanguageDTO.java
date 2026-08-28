package com.peach.setting.dto;

import java.io.Serial;

import com.peach.setting.comon.enums.LanguageGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 语言设置DTO。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 21:50
 * @Description 语言设置DTO
 */
@Data
public class LanguageDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 2069058615113074759L;

    @Schema(description = "语言ID")
    @NotBlank(groups = {LanguageGroup.UpdatetGroup.class}, message = "语言ID不能为空")
    private String id;

    @Schema(description = "语言编码")
    @NotBlank(groups = {LanguageGroup.InsertGroup.class}, message = "语言编码不能为空")
    private String languageCode;

    @Schema(description = "语言名称")
    @NotBlank(groups = {LanguageGroup.InsertGroup.class}, message = "语言名称不能为空")
    private String languageName;

    @Schema(description = "本地化名称")
    private String nativeName;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "默认标记")
    private Integer defaultFlag;

    @Schema(description = "排序")
    private Integer sortOrder;
}
