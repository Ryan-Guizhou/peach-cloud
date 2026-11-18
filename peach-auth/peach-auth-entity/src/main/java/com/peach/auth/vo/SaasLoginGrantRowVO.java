package com.peach.auth.vo;

import lombok.Data;

/**
 * 基于同一条当前成员角色绑定的登录授权事实。
 *
 * <p>由数据库按物理租户、成员、应用、自然年度、状态及版本过滤。
 * 每行来自单个有效角色绑定和其角色权限定义，不能跨不同绑定拼接授权。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 15:10
 */
@Data
public class SaasLoginGrantRowVO {

    private String roleRefId;
    private String roleId;
    private String roleCode;
    private String roleName;
    private String scopeType;

    private String permissionRefId;
    private String permissionId;
    private String permissionCode;
    private String resourceType;

    private String uiRefId;
    private String menuCode;
    private String menuName;
    private String menuIcon;
    private String routerCode;
    private String routerUrl;
    private String filePath;
}
