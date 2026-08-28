package com.peach.fileservice.vo;

import java.io.Serial;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 文件上传结果。
 * <p>用于封装文件上传操作完成后的返回结果，包含业务文件ID、文件名称、
 * 文件大小等基本信息，同时携带秒传标识和对象复用标识，
 * 便于前端判断上传方式及展示上传结果。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/19
 */
@Data
@Schema(description = "文件上传结果")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileUploadVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 3858559784270690914L;

    @Schema(description = "业务文件ID")
    private String fileId;

    @Schema(description = "是否秒传")
    private Boolean instantUpload;

    @Schema(description = "是否复用对象")
    private Boolean objectReused;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件大小")
    private Long fileSize;
}
