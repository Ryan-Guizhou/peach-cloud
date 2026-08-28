package com.peach.auth.qo;

import java.io.Serial;

import com.peach.common.PeachEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 用户查询参数。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/9 17:13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户查询参数")
public class UserQO extends PeachEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = -3295610003734086092L;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "默认租户ID")
    private String defaultTenantId;

    @Schema(description = "默认机构ID")
    private String defaultOrgId;

    @Schema(description = "用户ID集合")
    private List<String> userIdList;
}
