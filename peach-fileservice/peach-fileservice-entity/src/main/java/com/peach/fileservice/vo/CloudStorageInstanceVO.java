package com.peach.fileservice.vo;

import java.io.Serial;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peach.fileservice.entity.CloudStorageInstanceDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 云存储实例返回视图。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/9 00:01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "云存储实例返回视图")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CloudStorageInstanceVO extends CloudStorageInstanceDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 327197220037134973L;

    @Schema(description = "Masked secret key")
    private String secretKeyMasked;
}
