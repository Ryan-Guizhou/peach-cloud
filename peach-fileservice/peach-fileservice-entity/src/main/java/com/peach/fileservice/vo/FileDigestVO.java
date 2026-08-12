package com.peach.fileservice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 文件摘要结果。
 */
@Data
@Schema(description = "文件摘要结果")
public class FileDigestVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "摘要算法")
    private String algorithm;

    @Schema(description = "SHA-256 摘要")
    private String sha256;

    @Schema(description = "文件大小")
    private Long fileSize;
}
