package com.peach.auth.qo;

import java.io.Serial;

import com.peach.common.PeachEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * AuthLog查询参数。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:08
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "AuthLog查询参数")
public class AuthLogQO extends PeachEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 5451275970293738661L;

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "机构ID")
    private String orgId;

    @Schema(description = "操作人用户ID")
    private String operatorUserId;

    @Schema(description = "被授权用户ID")
    private String userId;
}
