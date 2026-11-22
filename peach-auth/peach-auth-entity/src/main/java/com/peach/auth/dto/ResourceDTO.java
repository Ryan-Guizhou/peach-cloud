package com.peach.auth.dto;

import java.io.Serial;

import com.peach.common.PeachGroup;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.io.Serializable;


/**
 * 资源DTO。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/9 16:45
 * @Description 资源DTO
 */
@Data
@Schema(description = "资源DTO")
public class ResourceDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 5940398568041986270L;

    @Schema(description = "资源ID")
    @NotBlank(message = "资源ID不能为空", groups = {PeachGroup.UpdateGroup.class})
    @Size(max = 32, message = "ID长度不能超过32", groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    private String resourceId;

    @Schema(description = "功能编码")
    @Size(max = 50, message = "功能编码长度不能超过50", groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    private String funcCode;

    @Schema(description = "资源类型")
    @NotBlank(message = "资源类型不能为空", groups = {PeachGroup.InsertGroup.class})
    @Size(max = 20, message = "资源类型长度不能超过20", groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    private String resourceType;

    @Schema(description = "资源编码")
    @NotBlank(message = "资源编码不能为空", groups = {PeachGroup.InsertGroup.class})
    @Size(max = 64, message = "资源编码长度不能超过64", groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    private String resourceCode;

    @Schema(description = "资源名称")
    @NotBlank(message = "资源名称不能为空", groups = {PeachGroup.InsertGroup.class})
    @Size(max = 32, message = "资源名称长度不能超过32", groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    private String resourceName;

    @Schema(description = "应用ID")
    @Size(max = 32, message = "应用ID长度不能超过32", groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    private String appId;

    @Schema(description = "是否删除")
    @Min(value = 0, groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    @Max(value = 1, groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    private Integer isDelete;
}
