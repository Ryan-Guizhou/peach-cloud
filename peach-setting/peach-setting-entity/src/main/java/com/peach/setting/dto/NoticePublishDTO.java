package com.peach.setting.dto;

import java.io.Serial;

import com.peach.setting.comon.enums.NoticeGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;

/**
 * 通知发布DTO。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 21:50
 * @Description 通知发布DTO
 */
@Data
public class NoticePublishDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 3649848894302126986L;

    @Schema(description = "通知ID")
    @NotBlank(groups = {NoticeGroup.PublishGroup.class}, message = "通知ID不能为空")
    private String id;

    @Schema(description = "接收人ID列表")
    private List<String> receiverIdList;
}

