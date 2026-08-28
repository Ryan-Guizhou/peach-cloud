package com.peach.auth.qo;

import java.io.Serial;

import com.peach.common.PeachEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * AuthResource查询参数。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:09
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "AuthResource查询参数")
public class AuthResourceQO extends PeachEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = -568284397715023676L;

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "机构ID")
    private String orgId;

    @Schema(description = "参与者代码")
    private String partyCode;

    @Schema(description = "功能代码")
    private String funcCode;

    @Schema(description = "操作类型")
    private String opType;

    @Schema(description = "资源编码")
    private String resourceCode;

    @Schema(description = "应用ID")
    private String appId;

    @Schema(description = "年度")
    private Integer fiscal;
}
