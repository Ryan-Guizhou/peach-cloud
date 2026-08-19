package com.peach.fileservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 文件上传预检查数据传输对象
 *
 * <p>用于文件上传前的校验与秒传检测，包含文件SHA256、MD5、大小、名称等核心信息，
 * 支持根据摘要判断文件是否已存在以实现秒传，同时携带业务类型、业务ID、存储提供方等
 * 业务关联信息，便于对文件合法性与存储策略进行预验证。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/19
 */
@Data
@Schema(description = "文件上传预检查参数")
public class FileUploadCheckDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "sha256不能为空")
    @Schema(description = "文件sha256", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sha256;

    @Schema(description = "文件md5")
    private String md5;

    @NotNull(message = "文件大小不能为空")
    @Min(value = 0L, message = "文件大小不能小于0")
    @Schema(description = "文件大小", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long fileSize;

    @NotBlank(message = "文件名不能为空")
    @Schema(description = "文件名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileName;

    @Schema(description = "显示文件名")
    private String displayName;

    @Schema(description = "内容类型")
    private String contentType;

    @NotBlank(message = "业务类型不能为空")
    @Schema(description = "业务类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private String bizType;

    @Schema(description = "业务ID")
    private String bizId;

    @Schema(description = "业务标签")
    private String bizTag;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "指定存储提供方")
    private String storageProvider;
}