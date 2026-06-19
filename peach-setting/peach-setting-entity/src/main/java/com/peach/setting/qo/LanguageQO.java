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
 * @Description 语言设置查询对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "语言设置查询对象")
public class LanguageQO extends PeachEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "语言ID")
    private String id;

    @Schema(description = "语言编码")
    private String languageCode;

    @Schema(description = "语言名称")
    private String languageName;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "默认标识")
    private Integer defaultFlag;
}
