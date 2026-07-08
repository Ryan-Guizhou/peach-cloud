package com.peach.fileservice.qo;

import com.peach.common.PeachEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 云存储实例查询参数.
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/9 00:01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "云存储实例查询参数")
public class CloudStorageInstanceQO extends PeachEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "存储实例ID")
    private String instanceId;

    @Schema(description = "存储实例名称")
    private String instanceName;

    @Schema(description = "存储类型")
    private String storeType;

    @Schema(description = "是否启用，0禁用1启用")
    private Integer enabled;

    @Schema(description = "是否内置，0否1是")
    private Integer builtIn;
}
