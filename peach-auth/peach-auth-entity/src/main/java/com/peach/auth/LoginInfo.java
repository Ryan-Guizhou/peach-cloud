package com.peach.auth;

import com.peach.auth.vo.MenuVO;
import com.peach.auth.vo.RoleVO;
import com.peach.auth.vo.RouterVO;
import com.peach.auth.vo.UserOrgVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 登录成功后的会话信息。
 * <p>承载当前用户的基础身份、机构、角色和菜单路由信息，供前端登录后初始化页面使用。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/21 21:42
 */
@Data
@Schema(description = "登录信息")
public class LoginInfo {

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户名称")
    private String userName;

    @Schema(description = "年度")
    private String fiscal;

    @Schema(description = "当前租户ID")
    private String tenantId;

    @Schema(description = "当前租户编码")
    private String tenantCode;

    @Schema(description = "当前租户名称")
    private String tenantName;

    @Schema(description = "当前机构ID")
    private String orgId;

    @Schema(description = "当前机构编码")
    private String orgCode;

    @Schema(description = "当前机构名称")
    private String orgName;

    @Schema(description = "登录凭证")
    private String token;

    @Schema(description = "是否默认密码")
    private Integer isDefaultPwd;

    @Schema(description = "用户机构关系列表")
    private List<UserOrgVO> userOrgList;

    @Schema(description = "已授权角色列表")
    private List<RoleVO> roleList;

    @Schema(description = "已授权菜单列表")
    private List<MenuVO> menuList;

    @Schema(description = "已授权路由列表")
    private List<RouterVO> routerList;
}
