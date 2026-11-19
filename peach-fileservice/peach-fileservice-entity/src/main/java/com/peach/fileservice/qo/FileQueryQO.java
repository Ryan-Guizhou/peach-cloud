package com.peach.fileservice.qo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.peach.common.PeachEntity;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 文件查询参数。
 * <p>用于文件信息的条件查询，支持按业务类型、业务ID、业务标签、
 * 文件状态等维度进行筛选，同时包含逻辑删除标记以支持软删除查询。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "文件查询参数")
public class FileQueryQO extends PeachEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 8962819040298914048L;

    @Schema(description = "业务类型")
    private String bizType;

    @Schema(description = "租户ID")
    private String tenantId;

    @JsonIgnore
    @Schema(description = "仅由服务端设置的物理租户ID")
    private String tenantRefId;

    @JsonIgnore
    @Schema(description = "仅由服务端设置的物理应用ID")
    private String appRefId;

    @JsonIgnore
    @Schema(description = "仅由服务端设置的文件所有者物理ID")
    private String ownerUserRefId;

    @JsonIgnore
    @Schema(description = "服务端授权的全租户可见范围")
    private boolean tenantAllVisible;

    @JsonIgnore
    @Schema(description = "服务端授权的本人可见范围")
    private boolean selfVisible;

    @JsonIgnore
    @Schema(description = "服务端授权的跨所有者有效机构范围")
    private List<String> organizationRefIds = List.of();

    @JsonIgnore
    @Schema(description = "服务端授权的本人有效机构范围")
    private List<String> selfOrganizationRefIds = List.of();

    @Schema(description = "机构ID")
    private String orgId;

    @Schema(description = "业务ID")
    private String bizId;

    @Schema(description = "业务标签")
    private String bizTag;

    @Schema(description = "文件状态")
    private String fileStatus;

    @Schema(description = "逻辑删除标记")
    private Integer isDelete;
}
