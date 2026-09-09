package com.peach.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 登录权限快照，承载后端统一装配的权限结果。
 */
@Data
@Schema(description = "登录权限快照")
public class LoginPermissionSnapshotVO {

    @Schema(description = "已授权角色列表")
    private List<RoleVO> roleList;

    @Schema(description = "已授权菜单列表")
    private List<MenuVO> menuList;

    @Schema(description = "已授权路由列表")
    private List<RouterVO> routerList;

    @Schema(description = "已授权资源列表")
    private List<AuthResourceVO> resourceList;

    @Schema(description = "按钮/API 权限编码扁平列表")
    private List<String> permissionList;

    @Schema(description = "API 资源编码集合")
    private List<String> apiResourceCodes;

    @Schema(description = "BUTTON 资源编码集合")
    private List<String> buttonResourceCodes;
}
