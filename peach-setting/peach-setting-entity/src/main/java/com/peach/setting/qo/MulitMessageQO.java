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
 * @Description 多语言消息查询对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "多语言消息查询对象")
public class MulitMessageQO extends PeachEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "消息Key")
    private String messageKey;

    @Schema(description = "语言区域")
    private String locale;

    @Schema(description = "模块编码")
    private String moduleCode;

    @Schema(description = "消息类型")
    private String messageType;

    @Schema(description = "使用范围")
    private String usageScope;

    @Schema(description = "状态")
    private Integer status;
}
