package com.peach.auth.qo;

import java.io.Serial;

import com.peach.common.PeachEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 资源查询参数。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:10
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "资源查询参数")
public class ResourceQO extends PeachEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 7671598811405720084L;

    @Schema(description = "资源ID")
    private String resourceId;

    @Schema(description = "功能编码")
    private String funcCode;

    @Schema(description = "资源类型")
    private String resourceType;

    @Schema(description = "资源编码")
    private String resourceCode;
}
