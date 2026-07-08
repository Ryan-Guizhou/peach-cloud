package com.peach.fileservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 云存储删除对象参数.
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/9 00:01
 */
@Data
@Schema(description = "云存储删除对象参数")
public class CloudStorageDeleteObjectDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "对象key", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "对象key不能为空")
    private String objectKey;
}
