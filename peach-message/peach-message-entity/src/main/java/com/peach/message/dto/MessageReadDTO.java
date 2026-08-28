package com.peach.message.dto;

import java.io.Serial;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 消息已读DTO。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/23 14:45
 * @Description 消息已读DTO
 */
@Data
public class MessageReadDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 2536997030690148829L;

    @Schema(description = "消息ID")
    @NotBlank(message = "消息ID不能为空")
    private String messageId;

    @Schema(description = "接收人ID")
    private String receiverId;
}
