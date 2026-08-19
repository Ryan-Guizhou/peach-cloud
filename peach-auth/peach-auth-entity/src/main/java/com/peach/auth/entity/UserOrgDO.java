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
 * 用户机构关系实体。
 * <p>描述一个用户可关联的多个机构，并记录默认机构和当前租户下的归属关系。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/8 14:10
 */
@Data
@Entity
@Table(name = "PEACH_USER_ORG")
@Schema(description = "用户机构关系实体")
@EqualsAndHashCode(callSuper = true)
public class UserOrgDO extends PeachDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ID")
    @Schema(description = "主键ID")
    private String id;

    @Column(name = "USER_ID")
    @Schema(description = "用户ID")
    private String userId;

    @Column(name = "TENANT_ID")
    @Schema(description = "租户ID")
    private String tenantId;

    @Column(name = "ORG_ID")
    @Schema(description = "机构ID")
    private String orgId;

    @Column(name = "IS_DEFAULT")
    @Schema(description = "是否默认机构")
    private Integer isDefault;

    @Column(name = "STATUS")
    @Schema(description = "状态")
    private String status;

    @Column(name = "IS_DELETE")
    @Schema(description = "是否删除")
    private Integer isDelete;

    @Column(name = "CREATED_TIME")
    @Schema(description = "创建时间")
    private String createdTime;

    @Column(name = "CREATOR_ID")
    @Schema(description = "创建人ID")
    private String creatorId;

    @Column(name = "MODIFY_TIME")
    @Schema(description = "修改时间")
    private String modifyTime;

    @Column(name = "MODIFIER_ID")
    @Schema(description = "修改人ID")
    private String modifierId;
}
