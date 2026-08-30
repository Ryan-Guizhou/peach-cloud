package com.peach.common;

import java.io.Serial;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;


/**
 * 当前用户上下文。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/5 19:20
 * @Description 当前用户上下文
 */
@Data
@Schema(description = "当前用户上下文")
public class CurrentUserDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 4682673142018982915L;

    @Schema(description = "当前用户ID")
    private String userId;

    @Schema(description = "当前用户语言")
    private String language;
}
