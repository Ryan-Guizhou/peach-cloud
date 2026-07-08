package com.peach.auth.entity;

import com.peach.common.PeachDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 17:36
 */
@Data
@Entity
@Table(name = "PEACH_AUTH_RESOURCE")
@Schema(description = "AuthResource实体")
public class AuthResourceDO extends PeachDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "RESOURCE_ID")
    @Schema(description = "逻辑ID")
    private String resourceId;

    @Column(name = "TENANT_ID")
    @Schema(description = "租户ID")
    private String tenantId;

    @Column(name = "ORG_ID")
    @Schema(description = "机构ID")
    private String orgId;

    @Column(name = "PARTY_CODE")
    @Schema(description = "参与者代码,角色编码")
    private String partyCode;

    @Column(name = "FUNC_CODE")
    @Schema(description = "功能代码")
    private String funcCode;

    @Column(name = "OP_TYPE")
    @Schema(description = "操作类型")
    private String opType;

    @Column(name = "RESOURCE_CODE")
    @Schema(description = "资源编码")
    private String resourceCode;

    @Column(name = "RESOURCE_NAME")
    @Schema(description = "资源名称")
    private String resourceName;

    @Column(name = "APP_ID")
    @Schema(description = "应用ID")
    private String appId;

    @Column(name = "IS_DELETE")
    @Schema(description = "是否删除")
    private Integer isDelete;

    @Column(name = "FISCAL")
    @Schema(description = "年度")
    private Integer fiscal;

}
