package com.peach.auth.qo;

import java.io.Serial;

import com.peach.common.PeachEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * AuthFunctionQO查询参数。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:08
  */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "AuthFunctionQO查询参数")
public class AuthFunctionQO extends PeachEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = -6300198139602166763L;

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "机构ID")
    private String orgId;

    @Schema(description = "参与者代码")
    private String partyCode;

    @Schema(description = "参与者类型")
    private String partyType;

    @Schema(description = "功能代码")
    private String funcCode;

    @Schema(description = "年度")
    private Integer fiscal;

    @Schema(description = "功能状态")
    private String state;

    @Schema(description = "应用ID")
    private String appId;
}
