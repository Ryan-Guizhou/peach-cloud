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
 * 机构实体。
 * <p>对应机构主数据表，记录租户下的组织编码、名称、状态以及基础审计信息。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/8 14:10
 */
@Data
@Entity
@Table(name = "PEACH_ORGANIZATION")
@Schema(description = "机构实体")
public class OrganizationDO extends PeachDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ORG_ID")
    @Schema(description = "机构ID")
    private String orgId;

    @Column(name = "TENANT_ID")
    @Schema(description = "租户ID")
    private String tenantId;

    @Column(name = "ORG_CODE")
    @Schema(description = "机构编码")
    private String orgCode;

    @Column(name = "ORG_NAME")
    @Schema(description = "机构名称")
    private String orgName;

    @Column(name = "STATUS")
    @Schema(description = "状态")
    private String status;

    @Column(name = "SORT_NUM")
    @Schema(description = "排序")
    private Integer sortNum;

    @Column(name = "IS_DELETE")
    @Schema(description = "是否删除")
    private Integer isDelete;
}
