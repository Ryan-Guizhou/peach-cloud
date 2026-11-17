package com.peach.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 已完成全局账号验证后可选择的租户与应用。
 *
 * <p>使用物理 ID 建立身份关联，业务标识和名称只供显示；不包含授权结果。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 17:50
 */
@Data
@Schema(description = "租户应用登录选项")
public class LoginTargetVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 322908074585820985L;

    @Schema(description = "租户物理主键")
    private String tenantRefId;

    @Schema(description = "租户永久业务标识")
    private String tenantId;

    @Schema(description = "租户名称")
    private String tenantName;

    @Schema(description = "应用物理主键")
    private String appRefId;

    @Schema(description = "应用永久业务标识")
    private String appId;

    @Schema(description = "应用名称")
    private String appName;
}
