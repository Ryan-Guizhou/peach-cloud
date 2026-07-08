package com.peach.fileservice.dto;

import com.peach.common.PeachEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 云存储对象查询参数.
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/9 00:01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "云存储对象查询参数")
public class CloudStorageListDTO extends PeachEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "路径")
    private String path;

    @Schema(description = "查询参数")
    private Boolean recursive;

    @Schema(description = "文件列表")
    private Boolean includeFiles;

    @Schema(description = "路径列表")
    private Boolean includeDirectories;
}
