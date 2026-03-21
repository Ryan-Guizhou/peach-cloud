package com.peach.auth.entity;

import com.peach.common.MapperGenerator;
import com.peach.common.PeachDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 17:36
 */
@Data
@Entity
@Table(name = "PEACH_MENU")
@Schema(description = "Menu实体")
public class MenuDO extends PeachDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "MENU_ID")
    @Schema(description = "菜单ID")
    private String menuId;

    @Column(name = "MENU_NAME")
    @Schema(description = "菜单名称")
    private String menuName;

    @Column(name = "MENU_CODE")
    @Schema(description = "菜单编码")
    private String menuCode;

    @Column(name = "IS_LEAF")
    @Schema(description = "是否叶子")
    private Integer isLeaf;

    @Column(name = "MENU_URL")
    @Schema(description = "菜单URL")
    private String menuUrl;

    @Column(name = "MENU_PARAM")
    @Schema(description = "菜单参数")
    private String menuParam;

    @Column(name = "PARENT_MENU_ID")
    @Schema(description = "父菜单ID")
    private String parentMenuId;

    @Column(name = "MENU_LEVEL")
    @Schema(description = "菜单级别")
    private String menuLevel;

    @Column(name = "SORT_NO")
    @Schema(description = "显示顺序")
    private Integer sortNo;

    @Column(name = "COLLAPSE_ICON")
    @Schema(description = "折叠图标")
    private String collapseIcon;

    @Column(name = "EXPAND_ICON")
    @Schema(description = "展开图标")
    private String expandIcon;

    @Column(name = "MENU_SEQ")
    @Schema(description = "菜单序列")
    private String menuSeq;

    @Column(name = "OPEN_MODE")
    @Schema(description = "打开方式")
    private String openMode;

    @Column(name = "SUBCOUNT")
    @Schema(description = "子菜单数量")
    private String subcount;

    @Column(name = "FUNC_CODE")
    @Schema(description = "功能编码")
    private String funcCode;

    @Column(name = "MENU_APP_ID")
    @Schema(description = "菜单应用ID")
    private String menuAppId;

    @Column(name = "APP_ID")
    @Schema(description = "应用ID")
    private String appId;

    @Column(name = "IS_DELETE")
    @Schema(description = "是否删除")
    private Integer isDelete;

    @Column(name = "IS_DISABLE")
    @Schema(description = "菜单显示是否禁用")
    private Integer isDisable;

    @Column(name = "IS_SHOW")
    @Schema(description = "是否显示:1显示,0不显示")
    private Integer isShow;

    @Column(name = "SF_BLANK")
    @Schema(description = "是否新页签打开菜单")
    private Integer sfBlank;

    @Column(name = "MENU_ICON")
    @Schema(description = "菜单图标")
    private String menuIcon;

    public static void main(String[] args) {
        System.out.println(MapperGenerator.genMapper(MenuDO.class));
    }

}