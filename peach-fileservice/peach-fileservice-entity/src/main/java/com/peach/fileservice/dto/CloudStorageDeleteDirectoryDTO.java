package com.peach.fileservice.dto;

import java.io.Serial;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 云存储删除路径参数。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/9 00:01
 */
@Data
@Schema(description = "云存储删除路径参数")
public class CloudStorageDeleteDirectoryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -6417645377646910631L;

    @Schema(description = "路径", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "路径不能为空")
    private String path;
}
