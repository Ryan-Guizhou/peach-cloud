package com.peach.fileservice.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 云存储浏览节点返回对象。
 *
 * <p>
 * 用于表示云存储目录浏览时返回的文件或目录节点信息，
 * 支持对象存储以及文件系统类型存储的统一展示。
 * </p>
 *
 * <p>
 * 例如：
 * OSS、OBS、COS、S3 等对象存储中的 Object，
 * 以及 NAS 等文件系统中的文件和目录。
 * </p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/7/9
 */
@Data
@Schema(description = "云存储浏览节点")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CloudStorageObjectNodeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 节点名称。
     *
     * <p>
     * 文件或目录名称，不包含完整路径。
     * </p>
     */
    @Schema(description = "节点名称")
    private String name;

    /**
     * 相对路径。
     *
     * <p>
     * 相对于当前浏览目录的路径。
     * </p>
     */
    @Schema(description = "相对路径")
    private String path;

    /**
     * 对象存储 Key。
     *
     * <p>
     * 对象存储中的唯一标识路径。
     * 对于文件系统类型存储，可以对应文件完整相对路径。
     * </p>
     */
    @Schema(description = "对象存储 Key")
    private String objectKey;

    /**
     * 是否为目录。
     *
     * <p>
     * true 表示目录，
     * false 表示文件对象。
     * </p>
     */
    @Schema(description = "是否为目录")
    private Boolean directory;

    /**
     * 文件大小。
     *
     * <p>
     * 单位：字节。
     * 目录类型通常为空。
     * </p>
     */
    @Schema(description = "文件大小，单位：字节")
    private Long size;

    /**
     * 对象 ETag 标识。
     *
     * <p>
     * 主要用于对象存储场景，
     * 可用于文件完整性校验以及对象版本识别。
     * </p>
     */
    @Schema(description = "对象 ETag 标识")
    private String etag;

    /**
     * 最后修改时间。
     *
     * <p>
     * 文件或对象最近一次修改时间。
     * </p>
     */
    @Schema(description = "最后修改时间")
    private String lastModified;

    /**
     * 内容类型。
     *
     * <p>
     * 对应文件 MIME 类型，
     * 例如：
     * text/plain、image/png、application/pdf。
     * </p>
     */
    @Schema(description = "内容类型")
    private String contentType;
}