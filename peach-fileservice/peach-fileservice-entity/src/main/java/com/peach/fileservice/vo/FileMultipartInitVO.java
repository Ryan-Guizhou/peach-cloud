package com.peach.fileservice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 文件分片上传初始化视图对象
 *
 * <p>用于封装分片上传初始化阶段的返回结果，包含秒传检测结果、
 * 文件业务ID、上传会话信息（sessionId、uploadId）、存储提供方与
 * Bucket/对象Key等底层存储元数据以及会话过期时间，
 * 为客户端后续分片上传提供完整的会话上下文。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/19
 */
@Data
@Schema(description = "分片上传初始化结果")
public class FileMultipartInitVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "是否秒传")
    private Boolean instantUpload;

    @Schema(description = "业务文件ID")
    private String fileId;

    @Schema(description = "上传会话ID")
    private String sessionId;

    @Schema(description = "底层uploadId")
    private String uploadId;

    @Schema(description = "存储提供方")
    private String providerName;

    @Schema(description = "bucket名称")
    private String bucketName;

    @Schema(description = "对象key")
    private String objectKey;

    @Schema(description = "会话过期时间")
    private String expiresAt;
}
