package com.peach.fileservice.vo;

import java.io.Serial;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 云存储上传结果。
 * <p>
 * 用于返回文件上传完成后的存储信息，
 * 包括存储提供方、存储桶、对象路径、文件大小以及访问地址等信息。
 * </p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/9
 */
@Data
@Schema(description = "云存储上传结果")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CloudStorageUploadVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 4304740160360999588L;

    /**
     * 存储提供方名称。
     *
     * <p>
     * 例如：
     * OSS、OBS、COS、S3、NAS 等。
     * </p>
     */
    @Schema(description = "存储提供方名称")
    private String providerName;

    /**
     * 存储桶名称。
     *
     * <p>
     * 对象存储场景对应 Bucket 名称，
     * 文件系统存储场景可以为空。
     * </p>
     */
    @Schema(description = "存储桶名称")
    private String bucketName;

    /**
     * 对象存储 Key。
     *
     * <p>
     * 表示文件在存储空间中的唯一路径，
     * 通常包含目录前缀和文件名称。
     * </p>
     */
    @Schema(description = "对象存储路径")
    private String objectKey;

    /**
     * 文件大小。
     *
     * <p>
     * 单位：字节。
     * </p>
     */
    @Schema(description = "文件大小，单位：字节")
    private Long size;

    /**
     * 文件访问地址。
     *
     * <p>
     * 根据存储配置可能返回：
     * <ul>
     *     <li>公共访问地址</li>
     *     <li>预签名访问地址</li>
     *     <li>CDN访问地址</li>
     * </ul>
     * </p>
     */
    @Schema(description = "文件访问地址")
    private String url;
}
