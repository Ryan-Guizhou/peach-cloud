package com.peach.auth.entity;

import jakarta.persistence.Version;
import com.peach.common.PeachDO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;



/**
 * 租户主表实体。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 17:36
 */
@Data
@Entity
@Table(name = "PEACH_TENANT")
@Schema(description = "租户主表实体")
@EqualsAndHashCode(callSuper = true)
public class TenantDO extends PeachDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 7397081470749548788L;

    @Id
    @Column(name = "ID")
    @Schema(description = "主键ID")
    private String id;

    @Column(name = "TENANT_ID")
    @Schema(description = "租户业务ID")
    private String tenantId;

    @Column(name = "TENANT_CODE")
    @Schema(description = "租户编码")
    private String tenantCode;

    @Column(name = "TENANT_NAME")
    @Schema(description = "租户名称")
    private String tenantName;

    @Column(name = "TENANT_TYPE")
    @Schema(description = "租户主体类型")
    private String tenantType;

    @Column(name = "STATUS")
    @Schema(description = "租户状态")
    private String status;

    @Column(name = "DESCRIPTION")
    @Schema(description = "备注")
    private String description;

    @Column(name = "CONTACT_NAME")
    @Schema(description = "主要联系人")
    private String contactName;

    @Column(name = "CONTACT_PHONE")
    @Schema(description = "联系人电话")
    private String contactPhone;

    @Column(name = "CONTACT_EMAIL")
    @Schema(description = "联系人邮箱")
    private String contactEmail;

    @Column(name = "ACTIVATED_TIME")
    @Schema(description = "首次激活时间")
    private String activatedTime;

    @Column(name = "CLOSED_AT")
    @Schema(description = "注销时间")
    private String closedAt;

    @Version
    @Column(name = "VERSION")
    @Schema(description = "乐观锁版本号")
    private Long version;

    @Column(name = "DELETED")
    @Schema(description = "逻辑删除标识")
    private Integer deleted;

    @Column(name = "CREATOR_ID")
    @Schema(description = "创建人ID")
    private String creatorId;

    @Column(name = "CREATOR_CODE")
    @Schema(description = "创建人账号")
    private String creatorCode;

    @Column(name = "CREATE_TIME")
    @Schema(description = "创建时间")
    private String createTime;

    @Column(name = "MODIFIER_ID")
    @Schema(description = "修改人ID")
    private String modifierId;

    @Column(name = "MODIFIER_CODE")
    @Schema(description = "修改人账号")
    private String modifierCode;

    @Column(name = "MODIFY_TIME")
    @Schema(description = "修改时间")
    private String modifyTime;

    @Column(name = "DELETER")
    @Schema(description = "删除人")
    private String deleter;

    @Column(name = "DELETE_TIME")
    @Schema(description = "删除时间")
    private String deleteTime;


}
