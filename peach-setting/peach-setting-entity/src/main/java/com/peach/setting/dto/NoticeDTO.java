package com.peach.setting.dto;

import com.peach.setting.comon.enums.NoticeGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 21:50
 * @Description 通知DTO
 */
@Data
public class NoticeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "通知ID")
    @NotBlank(groups = {NoticeGroup.UpdatetGroup.class}, message = "通知ID不能为空")
    private String id;

    @Schema(description = "通知编码")
    @NotBlank(groups = {NoticeGroup.InsertGroup.class}, message = "通知编码不能为空")
    private String noticeCode;

    @Schema(description = "标题消息Key")
    @NotBlank(groups = {NoticeGroup.InsertGroup.class}, message = "标题消息Key不能为空")
    private String titleMessageKey;

    @Schema(description = "内容消息Key")
    @NotBlank(groups = {NoticeGroup.InsertGroup.class}, message = "内容消息Key不能为空")
    private String contentMessageKey;

    @Schema(description = "通知类型")
    @NotBlank(groups = {NoticeGroup.InsertGroup.class}, message = "通知类型不能为空")
    private String noticeType;

    @Schema(description = "优先级")
    private Integer priority;

    @Schema(description = "发布状态")
    private String publishStatus;

    @Schema(description = "生效开始时间")
    private String effectiveFrom;

    @Schema(description = "生效结束时间")
    private String effectiveTo;

    @Schema(description = "已读数量")
    private Integer readCount;

    @Schema(description = "是否启用收件箱")
    private Integer inboxEnabled;

    @Schema(description = "状态")
    private Integer status;
}

