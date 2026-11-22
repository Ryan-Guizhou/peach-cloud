package com.peach.auth.service.support;

import com.peach.auth.vo.LoginPermissionSnapshotVO;

/**
 * 仅使用单角色且同时存在于已授权菜单、路由中的默认页面；多角色和失效值回退工作台。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 21:00
 */
public final class RoleDefaultRouteSupport {
    private RoleDefaultRouteSupport() { }
    /** 返回安全的本地已授权地址，不接受外部URL或多角色优先级猜测。 */
    public static String resolve(LoginPermissionSnapshotVO snapshot) {
        if (snapshot.getRoleList() == null || snapshot.getRoleList().size() != 1) return "/workspace";
        String value = snapshot.getRoleList().getFirst().getSkipUrl();
        if (value == null || !value.startsWith("/") || value.startsWith("//")) return "/workspace";
        if (snapshot.getMenuList() == null || snapshot.getRouterList() == null) return "/workspace";
        return snapshot.getMenuList().stream().anyMatch(menu -> value.equals(menu.getMenuUrl()))
                && snapshot.getRouterList().stream().anyMatch(route -> value.equals(route.getRouterUrl())) ? value : "/workspace";
    }
}
