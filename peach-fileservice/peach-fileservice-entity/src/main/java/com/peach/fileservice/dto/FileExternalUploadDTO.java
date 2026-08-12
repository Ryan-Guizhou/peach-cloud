package com.peach.fileservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 外部文件上传参数。
 *
 * <p>外部调用方不需要提交摘要；摘要由文件服务根据 multipart 内容计算。</p>
 */
@Data
@Schema(description = "外部文件上传参数")
public class FileExternalUploadDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "业务类型不能为空")
    @Schema(description = "业务类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private String bizType;

    @Schema(description = "业务ID")
    private String bizId;

    @Schema(description = "业务标签")
    private String bizTag;

    @Schema(description = "显示文件名")
    private String displayName;

    @Schema(description = "内容类型")
    private String contentType;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "指定存储提供方")
    private String storageProvider;
}
