package com.peach.fileservice.vo;

import java.io.Serial;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 文件摘要结果。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
@Data
@Schema(description = "文件摘要结果")
public class FileDigestVO implements Serializable {

    @Serial
    private static final long serialVersionUID = -1587434356937524234L;

    @Schema(description = "摘要算法")
    private String algorithm;

    @Schema(description = "SHA-256 摘要")
    private String sha256;

    @Schema(description = "文件大小")
    private Long fileSize;
}
