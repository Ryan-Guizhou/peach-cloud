package com.peach.setting.qo;

import java.io.Serial;

import com.peach.common.PeachEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 语言设置查询对象。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:40
 * @Description 语言设置查询对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "语言设置查询对象")
public class LanguageQO extends PeachEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1519612155376216225L;

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "机构ID")
    private String orgId;

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
