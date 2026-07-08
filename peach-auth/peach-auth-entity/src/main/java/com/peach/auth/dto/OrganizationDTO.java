package com.peach.auth.dto;

import com.peach.auth.group.OrganizationGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/8 14:10
 */
@Data
@Schema(description = "机构DTO")
public class OrganizationDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "机构ID")
    @NotBlank(message = "机构ID不能为空", groups = {OrganizationGroup.updateGroup.class})
    private String orgId;

    @Schema(description = "租户ID")
    @NotBlank(message = "租户ID不能为空", groups = {OrganizationGroup.insertGroup.class})
    private String tenantId;

    @Schema(description = "机构编码")
    @NotBlank(message = "机构编码不能为空", groups = {OrganizationGroup.insertGroup.class})
    @Size(max = 64, message = "机构编码长度不能超过64", groups = {OrganizationGroup.insertGroup.class, OrganizationGroup.updateGroup.class})
    private String orgCode;

    @Schema(description = "机构名称")
    @NotBlank(message = "机构名称不能为空", groups = {OrganizationGroup.insertGroup.class})
    @Size(max = 128, message = "机构名称长度不能超过128", groups = {OrganizationGroup.insertGroup.class, OrganizationGroup.updateGroup.class})
    private String orgName;

    @Schema(description = "状态")
    @Size(max = 16, message = "状态长度不能超过16", groups = {OrganizationGroup.insertGroup.class, OrganizationGroup.updateGroup.class})
    private String status;

    @Schema(description = "排序")
    private Integer sortNum;

    @Schema(description = "是否删除")
    private Integer isDelete;
}
