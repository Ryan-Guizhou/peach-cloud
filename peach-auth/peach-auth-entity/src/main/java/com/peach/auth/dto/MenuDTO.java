package com.peach.auth.dto;

import com.peach.auth.group.MenuGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/9 16:45
 * @Description 菜单DTO
 */
@Data
@Schema(description = "菜单DTO")
public class MenuDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "菜单ID")
    @NotBlank(message = "菜单ID不能为空", groups = {MenuGroup.insertGroup.class, MenuGroup.updateGroup.class})
    private String menuId;

    @Schema(description = "菜单名称")
    @NotBlank(message = "菜单名称不能为空", groups = {MenuGroup.insertGroup.class})
    @Size(max = 100, message = "菜单名称长度不能超过100", groups = {MenuGroup.insertGroup.class, MenuGroup.updateGroup.class})
    private String menuName;

    @Schema(description = "菜单编码")
    @NotBlank(message = "菜单编码不能为空", groups = {MenuGroup.insertGroup.class})
    @Size(max = 50, message = "菜单编码长度不能超过50", groups = {MenuGroup.insertGroup.class, MenuGroup.updateGroup.class})
    private String menuCode;

    @Schema(description = "是否叶子")
    private Integer isLeaf;

    @Schema(description = "菜单URL")
    @Size(max = 255, message = "菜单URL长度不能超过255", groups = {MenuGroup.insertGroup.class, MenuGroup.updateGroup.class})
    private String menuUrl;

    @Schema(description = "菜单参数")
    @Size(max = 255, message = "菜单参数长度不能超过255", groups = {MenuGroup.insertGroup.class, MenuGroup.updateGroup.class})
    private String menuParam;

    @Schema(description = "父菜单ID")
    @Size(max = 50, message = "父菜单ID长度不能超过50", groups = {MenuGroup.insertGroup.class, MenuGroup.updateGroup.class})
    private String parentMenuId;

    @Schema(description = "菜单级别")
    @Size(max = 20, message = "菜单级别长度不能超过20", groups = {MenuGroup.insertGroup.class, MenuGroup.updateGroup.class})
    private String menuLevel;

    @Schema(description = "显示顺序")
    private Integer sortNo;

    @Schema(description = "折叠图标")
    @Size(max = 100, message = "折叠图标长度不能超过100", groups = {MenuGroup.insertGroup.class, MenuGroup.updateGroup.class})
    private String collapseIcon;

    @Schema(description = "展开图标")
    @Size(max = 100, message = "展开图标长度不能超过100", groups = {MenuGroup.insertGroup.class, MenuGroup.updateGroup.class})
    private String expandIcon;

    @Schema(description = "菜单序列")
    @Size(max = 100, message = "菜单序列长度不能超过100", groups = {MenuGroup.insertGroup.class, MenuGroup.updateGroup.class})
    private String menuSeq;

    @Schema(description = "打开方式")
    @Size(max = 50, message = "打开方式长度不能超过50", groups = {MenuGroup.insertGroup.class, MenuGroup.updateGroup.class})
    private String openMode;

    @Schema(description = "子菜单数")
    @Size(max = 20, message = "子菜单数长度不能超过20", groups = {MenuGroup.insertGroup.class, MenuGroup.updateGroup.class})
    private String subcount;

    @Schema(description = "功能编码")
    @Size(max = 50, message = "功能编码长度不能超过50", groups = {MenuGroup.insertGroup.class, MenuGroup.updateGroup.class})
    private String funcCode;

    @Schema(description = "菜单应用ID")
    @Size(max = 50, message = "菜单应用ID长度不能超过50", groups = {MenuGroup.insertGroup.class, MenuGroup.updateGroup.class})
    private String menuAppId;

    @Schema(description = "应用ID")
    @Size(max = 50, message = "应用ID长度不能超过50", groups = {MenuGroup.insertGroup.class, MenuGroup.updateGroup.class})
    private String appId;

    @Schema(description = "是否删除")
    private Integer isDelete;

    @Schema(description = "是否禁用")
    private Integer isDisable;

    @Schema(description = "是否显示")
    private Integer isShow;

    @Schema(description = "是否新页签打开")
    private Integer sfBlank;

    @Schema(description = "菜单图标")
    @Size(max = 100, message = "菜单图标长度不能超过100", groups = {MenuGroup.insertGroup.class, MenuGroup.updateGroup.class})
    private String menuIcon;
}
