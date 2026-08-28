package com.peach.message.dto;

import java.io.Serial;

import com.peach.message.common.enums.MessageEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * WebSocket 推送消息DTO。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/23 14:45
 * @Description WebSocket 推送消息DTO
 */
@Data
public class WebSocketMessageDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -1383956385113101596L;

    @Schema(description = "推送模式")
    private MessageEnum.WsPushMode mode;

    @Schema(description = "事件类型")
    private String type;

    @Schema(description = "接收用户ID列表")
    private List<String> userIds;

    @Schema(description = "订阅频道列表")
    private List<String> channels;

    @Schema(description = "链路追踪ID")
    private String traceId;

    @Schema(description = "事件时间戳")
    private Long timestamp;

    @Schema(description = "消息体")
    private transient Object payload;
}
