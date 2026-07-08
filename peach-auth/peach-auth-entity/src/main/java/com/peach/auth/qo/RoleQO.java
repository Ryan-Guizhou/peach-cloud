package com.peach.auth.qo;

import com.peach.common.PeachEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:10
 */
@Data
@Schema(description = "角色查询参数")
public class RoleQO extends PeachEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "年度")
    private Integer fiscal;

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "机构ID")
    private String orgId;

    @Schema(description = "用户账号")
    private String userCode;

    @Schema(description = "角色ID")
    private String roleId;

    @Schema(description = "角色编码")
    private String roleCode;

}
