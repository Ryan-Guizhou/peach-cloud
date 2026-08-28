package com.peach.fileservice.dto;

import java.io.Serial;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 分片上传地址请求参数。
 * <p>用于大文件分片上传场景中，客户端请求获取指定分片的预签名上传地址。
 * 通过上传会话ID关联已初始化的分片上传任务，并通过分片序号定位具体的文件分片，
 * 从而获取该分片对应的临时上传URL。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/19
 */
@Data
@Schema(description = "分片上传地址请求参数")
public class FileMultipartPartUrlDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 347916062736225496L;

    @NotBlank(message = "sessionId不能为空")
    @Schema(description = "上传会话ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sessionId;

    @Min(value = 1L, message = "partNumber必须大于0")
    @Schema(description = "分片序号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer partNumber;
}
