package com.peach.auth.entity;

import com.peach.common.PeachDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 17:36
 */
@Data
@Entity
@Table(name = "PEACH_AUTH_PARTY")
@Schema(description = "AuthParty实体")
@EqualsAndHashCode(callSuper = true)
public class AuthPartyDO extends PeachDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ID")
    @Schema(description = "主键ID")
    private String id;

    @Column(name = "TENANT_ID")
    @Schema(description = "租户ID")
    private String tenantId;

    @Column(name = "ORG_ID")
    @Schema(description = "机构ID")
    private String orgId;

    @Column(name = "ROLE_CODE")
    @Schema(description = "角色代码")
    private String roleCode;

    @Column(name = "ROLE_TYPE")
    @Schema(description = "角色类型")
    private String roleType;

    @Column(name = "FISCAL")
    @Schema(description = "年度")
    private Integer fiscal;

    @Column(name = "PARTY_CODE")
    @Schema(description = "参与者代码")
    private String partyCode;

    @Column(name = "PARTY_TYPE")
    @Schema(description = "参与者类型")
    private String partyType;

    @Column(name = "IS_DELETE")
    @Schema(description = "是否删除")
    private Integer isDelete;


}
