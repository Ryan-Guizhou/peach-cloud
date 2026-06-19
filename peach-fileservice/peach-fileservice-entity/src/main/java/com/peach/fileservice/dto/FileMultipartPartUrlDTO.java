package com.peach.fileservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 分片上传地址请求数据传输对象
 *
 * <p>用于大文件分片上传场景中，客户端请求获取指定分片的预签名上传地址。
 * 通过上传会话ID关联已初始化的分片上传任务，并通过分片序号定位具体的文件分片，
 * 从而获取该分片对应的临时上传URL。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/19
 */
@Data
@Schema(description = "分片上传地址请求参数")
public class FileMultipartPartUrlDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "sessionId不能为空")
    @Schema(description = "上传会话ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sessionId;

    @Min(value = 1L, message = "partNumber必须大于0")
    @Schema(description = "分片序号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer partNumber;
}
