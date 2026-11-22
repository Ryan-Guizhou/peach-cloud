package com.peach.auth.vo;

import lombok.Data;
import java.util.List;

/**
 * 角色工作区的完整授权快照；界面筛选不截断快照数据。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 21:00
 */
@Data
public class RoleWorkspaceVO {
    private RoleVO role;
    private List<FunctionVO> functions;
    private List<ResourceVO> resources;
    private List<String> funcCodes;
    private List<AuthResourceVO> resourceGrants;
    private List<MenuVO> menus;
    private String revision;
}
