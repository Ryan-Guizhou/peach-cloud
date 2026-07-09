package com.peach.fileservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 云存储上传参数.
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/9 00:01
 */
@Data
@Schema(description = "云存储上传参数")
public class CloudStorageUploadDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "路径")
    private String targetPath;
}
