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
 * 租户成员邀请记录，只有待接受的有效邀请参与活动唯一约束。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 15:30
 */
@Data
@Entity
@Table(name = "PEACH_TENANT_INVITATION")
@Schema(description = "租户成员邀请")
@EqualsAndHashCode(callSuper = true)
public class TenantInvitationDO extends PeachDO {

    @Serial
    private static final long serialVersionUID = -452903356867160253L;

    @Id
    @Column(name = "ID")
    private String id;

    @Column(name = "INVITATION_ID")
    private String invitationId;

    @Column(name = "TENANT_REF_ID")
    private String tenantRefId;

    @Column(name = "INVITER_MEMBER_REF_ID")
    private String inviterMemberRefId;

    @Column(name = "INVITEE_USER_REF_ID")
    private String inviteeUserRefId;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "EXPIRES_AT")
    private LocalDateTime expiresAt;

    @Column(name = "IS_DELETE")
    private Integer isDelete;

    @Column(name = "DELETED_AT")
    private LocalDateTime deletedAt;

    @Column(name = "DELETED_BY_REF_ID")
    private String deletedByRefId;

    @Column(name = "VERSION")
    private Long version;
}
