package com.peach.auth.dto;

import java.io.Serial;

import com.peach.common.PeachGroup;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;


/**
 * 功能DTO。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/9 16:45
 * @Description 功能DTO
 */
@Data
@Schema(description = "功能DTO")
public class FunctionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -3702934397198638628L;

    @Schema(description = "功能ID")
    @NotBlank(message = "功能ID不能为空", groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    private String funcId;

    @Schema(description = "功能编码")
    @NotBlank(message = "功能编码不能为空", groups = {PeachGroup.InsertGroup.class})
    @Size(max = 50, message = "功能编码长度不能超过50", groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    private String funcCode;

    @Schema(description = "父功能编码")
    @Size(max = 50, message = "父功能编码长度不能超过50", groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    private String parentFuncCode;

    @Schema(description = "功能名称")
    @NotBlank(message = "功能名称不能为空", groups = {PeachGroup.InsertGroup.class})
    @Size(max = 100, message = "功能名称长度不能超过100", groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    private String funcName;

    @Schema(description = "功能描述")
    @Size(max = 255, message = "功能描述长度不能超过255", groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    private String funcDesc;

    @Schema(description = "功能URL")
    @Size(max = 255, message = "功能URL长度不能超过255", groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    private String funcUrl;

    @Schema(description = "功能序列")
    @Size(max = 100, message = "功能序列长度不能超过100", groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    private String funcSeq;

    @Schema(description = "功能类型")
    @Size(max = 50, message = "功能类型长度不能超过50", groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    private String funcType;

    @Schema(description = "是否菜单")
    private Integer isMenu;

    @Schema(description = "是否授权访问")
    private Integer isAuthorize;

    @Schema(description = "应用ID")
    @Size(max = 50, message = "应用ID长度不能超过50", groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    private String appId;

    @Schema(description = "是否禁用")
    private Integer isDisable;

    @Schema(description = "是否删除")
    private Integer isDelete;
}
