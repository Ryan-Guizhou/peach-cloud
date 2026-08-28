package com.peach.fileservice.dto;

import java.io.Serial;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 云存储删除对象参数。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/9 00:01
 */
@Data
@Schema(description = "云存储删除对象参数")
public class CloudStorageDeleteObjectDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 7608212210186163799L;

    @Schema(description = "对象key", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "对象key不能为空")
    private String objectKey;
}
