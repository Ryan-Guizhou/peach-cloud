package com.peach.auth;

import com.peach.auth.entity.MenuDO;
import com.peach.auth.vo.MenuVO;
import com.peach.auth.vo.RoleVO;
import com.peach.auth.vo.RouterVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
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

    @Schema(description = "登录凭证")
    private String token;

    @Schema(description = "是否默认密码")
    private Integer isDefaultPwd;

    @Schema(description = "已授权角色列表")
    private List<RoleVO> roleList;

    @Schema(description = "已授权菜单列表")
    private List<MenuVO> menuList;

    @Schema(description = "已授权路由列表")
    private List<RouterVO> routerList;
}
