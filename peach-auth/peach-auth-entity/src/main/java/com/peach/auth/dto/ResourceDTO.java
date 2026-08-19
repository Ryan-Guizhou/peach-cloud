package com.peach.auth.dto;

import com.peach.auth.group.ResourceGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;


/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/9 16:45
 * @Description 资源DTO
 */
@Data
@Schema(description = "资源DTO")
public class ResourceDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "资源ID")
    @NotBlank(message = "资源ID不能为空", groups = {ResourceGroup.insertGroup.class, ResourceGroup.updateGroup.class})
    private String resourceId;

    @Schema(description = "功能编码")
    @Size(max = 50, message = "功能编码长度不能超过50", groups = {ResourceGroup.insertGroup.class, ResourceGroup.updateGroup.class})
    private String funcCode;

    @Schema(description = "资源类型")
    @NotBlank(message = "资源类型不能为空", groups = {ResourceGroup.insertGroup.class})
    @Size(max = 50, message = "资源类型长度不能超过50", groups = {ResourceGroup.insertGroup.class, ResourceGroup.updateGroup.class})
    private String resourceType;

    @Schema(description = "资源编码")
    @NotBlank(message = "资源编码不能为空", groups = {ResourceGroup.insertGroup.class})
    @Size(max = 50, message = "资源编码长度不能超过50", groups = {ResourceGroup.insertGroup.class, ResourceGroup.updateGroup.class})
    private String resourceCode;

    @Schema(description = "资源名称")
    @NotBlank(message = "资源名称不能为空", groups = {ResourceGroup.insertGroup.class})
    @Size(max = 100, message = "资源名称长度不能超过100", groups = {ResourceGroup.insertGroup.class, ResourceGroup.updateGroup.class})
    private String resourceName;

    @Schema(description = "应用ID")
    @Size(max = 50, message = "应用ID长度不能超过50", groups = {ResourceGroup.insertGroup.class, ResourceGroup.updateGroup.class})
    private String appId;

    @Schema(description = "是否删除")
    private Integer isDelete;
}
