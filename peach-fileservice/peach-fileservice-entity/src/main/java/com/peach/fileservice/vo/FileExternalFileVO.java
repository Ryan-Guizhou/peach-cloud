package com.peach.fileservice.vo;

import java.io.Serial;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 外部文件详情。
 * <p>不返回 bucket、objectKey 等内部存储定位信息。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
@Data
@Schema(description = "外部文件详情")
public class FileExternalFileVO implements Serializable {

    @Serial
    private static final long serialVersionUID = -6313501623488767161L;

    @Schema(description = "业务文件ID")
    private String fileId;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "显示文件名")
    private String displayName;

    @Schema(description = "内容类型")
    private String contentType;

    @Schema(description = "文件大小")
    private Long fileSize;

    @Schema(description = "文件状态")
    private String fileStatus;
}
