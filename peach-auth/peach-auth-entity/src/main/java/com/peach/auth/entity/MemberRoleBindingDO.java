package com.peach.auth.entity;

import com.peach.common.PeachDO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 成员针对单个应用与自然年度的独立角色绑定。
 *
 * <p>租户范围不带 SCOPE_REF_ID；机构及机构树范围必须指向已启用机构物理 ID。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 18:10
 */
@Data
@Entity
@Table(name = "PEACH_MEMBER_ROLE_BINDING")
@Schema(description = "成员角色授权绑定")
@EqualsAndHashCode(callSuper = true)
public class MemberRoleBindingDO extends PeachDO {

    @Serial
    private static final long serialVersionUID = 781525732229200467L;

    @Id
    @Column(name = "ID")
    private String id;

    @Column(name = "TENANT_REF_ID")
    private String tenantRefId;

    @Column(name = "MEMBER_REF_ID")
    private String memberRefId;

    @Column(name = "APP_REF_ID")
    private String appRefId;

    @Column(name = "ROLE_REF_ID")
    private String roleRefId;

    @Column(name = "AUTH_YEAR")
    private Integer authYear;

    @Column(name = "SCOPE_TYPE")
    private String scopeType;

    @Column(name = "SCOPE_REF_ID")
    private String scopeRefId;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "IS_DELETE")
    private Integer isDelete;

    @Column(name = "VERSION")
    private Long version;

    @Column(name = "DELETED_AT")
    private LocalDateTime deletedAt;
}
