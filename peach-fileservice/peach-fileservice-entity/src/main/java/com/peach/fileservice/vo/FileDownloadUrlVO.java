package com.peach.fileservice.vo;

import java.io.Serial;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

/**
 * 文件下载地址结果。
 * <p>用于返回文件下载相关信息，包含文件ID、下载地址和过期时间，
 * 支持客户端获取临时下载链接并感知链接有效期。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/19
 */
@Data
@Schema(description = "文件下载地址结果")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileDownloadUrlVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 8729080558628122598L;

    @Schema(description = "业务文件ID")
    private String fileId;

    @Schema(description = "下载地址")
    private String url;

    @Schema(description = "过期时间")
    private String expiresAt;
}
