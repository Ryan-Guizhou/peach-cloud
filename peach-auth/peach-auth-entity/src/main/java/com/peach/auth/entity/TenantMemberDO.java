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
 * 用户在指定租户内的独立成员身份。
 *
 * <p>用户身份为全局实体，成员关系属于租户；真实组织归属与角色授权分别维护，
 * 不从用户组织关系推导成员资格或权限。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 15:30
 */
@Data
@Entity
@Table(name = "PEACH_TENANT_MEMBER")
@Schema(description = "租户成员")
@EqualsAndHashCode(callSuper = true)
public class TenantMemberDO extends PeachDO {

    @Serial
    private static final long serialVersionUID = -835428181650187596L;

    @Id
    @Column(name = "ID")
    private String id;

    @Column(name = "MEMBER_ID")
    private String memberId;

    @Column(name = "TENANT_REF_ID")
    private String tenantRefId;

    @Column(name = "USER_REF_ID")
    private String userRefId;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "IS_DELETE")
    private Integer isDelete;

    @Column(name = "DELETED_AT")
    private LocalDateTime deletedAt;

    @Column(name = "DELETED_BY_REF_ID")
    private String deletedByRefId;

    @Column(name = "VERSION")
    private Long version;

    @Column(name = "AUTH_VERSION")
    @Schema(description = "角色及权限授权版本，撤销或修改后只递增")
    private Long authVersion;
}
