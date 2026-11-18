package com.peach.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 成功开通租户后的物理身份与永久业务编号。
 *
 * <p>仅返回开通结果，不回传操作者凭证、角色授权快照或敏感配置。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 18:30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "租户开通结果")
public class SaasTenantCreatedVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 219002L;

    private String tenantRefId;

    private String tenantId;

    private String appRefId;

    private String appId;

    private String organizationRefId;

    private String organizationId;

    private String ownerMemberRefId;
}
