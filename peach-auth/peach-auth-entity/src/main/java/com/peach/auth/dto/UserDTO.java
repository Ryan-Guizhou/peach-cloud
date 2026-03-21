package com.peach.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/9 16:45
 */
@Data
@Schema(description = "用户DTO")
public class UserDTO implements Serializable {

    private static final long serialVersionUID = 5268266371854905203L;

    @Schema(description = "用户ID")
    private String id;

    @Schema(description = "用户名")
    private String username;
}
