package com.peach.fileservice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 文件下载地址视图对象
 *
 * <p>用于返回文件下载相关信息，包含文件ID、下载地址和过期时间，
 * 支持客户端获取临时下载链接并感知链接有效期。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/19
 */
@Data
@Schema(description = "文件下载地址结果")
public class FileDownloadUrlVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "业务文件ID")
    private String fileId;

    @Schema(description = "下载地址")
    private String url;

    @Schema(description = "过期时间")
    private String expiresAt;
}
