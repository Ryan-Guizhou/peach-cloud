package com.peach.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/23 14:45
 * @Description 消息撤销DTO
 */
@Data
public class MessageRevokeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "来源编码")
    @NotBlank(message = "来源编码不能为空")
    private String sourceCode;
}
