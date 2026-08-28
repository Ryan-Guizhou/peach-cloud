package com.peach.fileservice.vo;

import java.io.Serial;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 分片上传地址结果。
 * <p>用于封装文件分片上传时每个分片的上传地址信息，包含上传会话标识、
 * 底层上传标识、分片序号、预签名上传地址及地址过期时间等字段，
 * 为客户端逐片上传提供所需的地址凭证与元数据。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/19
 */
@Data
@Schema(description = "分片上传地址结果")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileMultipartPartVO implements Serializable {

    @Serial
    private static final long serialVersionUID = -8956415828248676243L;

    @Schema(description = "上传会话ID")
    private String sessionId;

    @Schema(description = "底层uploadId")
    private String uploadId;

    @Schema(description = "分片序号")
    private Integer partNumber;

    @Schema(description = "上传地址")
    private String uploadUrl;

    @Schema(description = "地址过期时间")
    private String expiresAt;
}
