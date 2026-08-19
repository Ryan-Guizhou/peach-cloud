package com.peach.message.dto;

import com.peach.message.common.enums.MessageEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/23 14:45
 * @Description 消息发布DTO
 */
@Data
public class MessagePublishDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "消息类型")
    private String messageType;

    @Schema(description = "接收人类型")
    private MessageEnum.ReceiverType receiverType;

    @Schema(description = "接收人ID列表")
    @NotEmpty(message = "接收人ID列表不能为空")
    private List<String> receiverIds;

    @Schema(description = "消息标题")
    private String title;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "标题消息Key")
    private String titleMessageKey;

    @Schema(description = "内容消息Key")
    private String contentMessageKey;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "来源编码")
    private String sourceCode;

    @Schema(description = "业务类型")
    private String bizType;

    @Schema(description = "业务ID")
    private String bizId;

    @Schema(description = "前端跳转地址")
    private String url;

    @Schema(description = "扩展数据")
    private Map<String, Object> extra;

    @Schema(description = "是否需要站内信持久化")
    private Boolean persistent;

    @Schema(description = "是否需要 WebSocket 实时推送")
    private Boolean realtime;

    @Schema(description = "消息优先级")
    private MessageEnum.MessagePriority priority;
}
