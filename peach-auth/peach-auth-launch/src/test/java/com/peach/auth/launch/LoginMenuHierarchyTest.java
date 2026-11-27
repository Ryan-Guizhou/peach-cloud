package com.peach.auth.launch;

import com.peach.auth.dao.*;
import com.peach.auth.service.assembler.LoginPermissionAssembler;
import com.peach.auth.vo.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * 菜单分组仅补齐已授权叶子的目录祖先，不暴露其他叶子。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 20:00
 */
class LoginMenuHierarchyTest {
    @Test void snapshotIncludesOnlyAuthorisedLeavesAndTheirAncestors() {
        var functions = mock(AuthFunctionDao.class); var resources = mock(AuthResourceDao.class);
        var menus = mock(MenuDao.class); var routers = mock(RouterDao.class);
        var assembler = new LoginPermissionAssembler(functions, resources, menus, routers);
        var role = new RoleVO(); role.setRoleCode("auditor");
        var context = new UserOrgVO(); context.setTenantId("tenant"); context.setOrgId("org");
        var grant = new AuthFunctionVO(); grant.setFuncCode("SYS_LOGIN_LOG");
        when(functions.selectByPartyCodes(any(), any(), any())).thenReturn(List.of(grant));
        var leaf = new MenuVO(); leaf.setMenuId("leaf"); leaf.setParentMenuId("audit"); leaf.setIsLeaf(1);
        var parent = new MenuVO(); parent.setMenuId("audit"); parent.setIsLeaf(0);
        var unrelated = new MenuVO(); unrelated.setMenuId("storage"); unrelated.setIsLeaf(0);
        when(menus.selectByFuncCodes(any(), any())).thenReturn(List.of(leaf));
        when(menus.selectCatalogs(any())).thenReturn(List.of(parent, unrelated));
        var snapshot = assembler.assemble(context, List.of(role));
        assertEquals(List.of("leaf", "audit"), snapshot.getMenuList().stream().map(MenuVO::getMenuId).toList());
    }
}
