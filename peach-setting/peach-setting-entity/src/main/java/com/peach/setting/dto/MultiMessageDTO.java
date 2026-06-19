package com.peach.setting.dto;

import com.peach.setting.comon.enums.MultiMessageGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 21:50
 * @Description 多语言消息DTO
 */
@Data
public class MultiMessageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "消息ID")
    @NotBlank(groups = {MultiMessageGroup.UpdatetGroup.class}, message = "消息ID不能为空")
    private String id;

    @Schema(description = "消息Key")
    @NotBlank(groups = {MultiMessageGroup.InsertGroup.class}, message = "消息Key不能为空")
    private String messageKey;

    @Schema(description = "语言区域")
    @NotBlank(groups = {MultiMessageGroup.InsertGroup.class}, message = "语言区域不能为空")
    private String locale;

    @Schema(description = "消息内容")
    @NotBlank(groups = {MultiMessageGroup.InsertGroup.class}, message = "消息内容不能为空")
    private String messageContent;

    @Schema(description = "模块编码")
    private String moduleCode;

    @Schema(description = "消息类型")
    private String messageType;

    @Schema(description = "使用范围")
    private String usageScope;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "状态")
    private Integer status;
}
