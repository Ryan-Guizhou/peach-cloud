package com.peach.auth.launch;

import com.peach.auth.dao.*;
import com.peach.auth.dto.RoleWorkspaceDTO;
import com.peach.auth.service.impl.RoleWorkspaceService;
import com.peach.auth.service.support.*;
import com.peach.auth.vo.*;
import com.peach.common.exception.BusinessException;
import com.peach.satoken.context.*;
import org.junit.jupiter.api.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * 角色工作区的服务端权限、租户隔离、默认菜单和事务后缓存刷新回归。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 21:00
 */
class RoleWorkspaceSecurityTest {
    RoleDao roles; RoleWorkspaceDao dao; FunctionDao functions; ResourceDao resources;
    LoginPermissionCacheRefresher refresh; RoleWorkspaceService service; RoleVO role;
    @BeforeEach void setup() {
        roles=mock(RoleDao.class);dao=mock(RoleWorkspaceDao.class);functions=mock(FunctionDao.class);resources=mock(ResourceDao.class);refresh=mock(LoginPermissionCacheRefresher.class);
        service=new RoleWorkspaceService(roles,dao,functions,resources,refresh,mock(AuthLogSupport.class));
        UserContext actor=new UserContext();actor.setTenantId("tenant");actor.setOrgId("org");actor.setUserId("user");actor.setFiscal("2026");SecurityContextHolder.set(actor);
        role=new RoleVO();role.setRoleId("id");role.setRoleCode("ROLE");role.setTenantId("tenant");role.setOrgId("org");role.setFiscal(2026);
    }
    @AfterEach void clear() { SecurityContextHolder.clear(); if(TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization(); }
    void allow() {
        when(dao.hasPermission(any(),anyString())).thenReturn(1);when(roles.selectById("id")).thenReturn(role);when(dao.lockRole(eq("id"),any())).thenReturn(role);
        var f=new FunctionVO();f.setFuncCode("VALID");when(functions.selectByQO(any())).thenReturn(List.of(f));
    }
    @Test void missingPermissionStopsBeforeReadingRoleData() {
        assertThrows(BusinessException.class,()->service.get("id"));verifyNoInteractions(roles,functions,resources);
    }
    @Test void authorisedViewerCannotReadAnotherTenantOrYear() {
        allow();role.setTenantId("other");assertThrows(BusinessException.class,()->service.get("id"));
        role.setTenantId("tenant");role.setFiscal(2025);assertThrows(BusinessException.class,()->service.get("id"));
        verifyNoInteractions(functions,resources);
    }
    @Test void unknownNewFunctionCannotBeGrantedAndNoRowsAreDeleted() {
        allow();var current=service.get("id");RoleWorkspaceDTO command=new RoleWorkspaceDTO();command.setRoleId("id");command.setRevision(current.getRevision());command.setFuncCodes(List.of("OTHER_TENANT_FUNCTION"));command.setResourceKeys(List.of());
        assertThrows(BusinessException.class,()->service.saveGrants(command));verify(dao,never()).deleteFunctions(any());verify(dao,never()).deleteResources(any());
    }
    @Test void defaultMenuRejectsExternalAndUnassignedPages() {
        allow();var menu=new MenuVO();menu.setMenuUrl("/allowed");when(dao.menus(any())).thenReturn(List.of(menu));
        var current=service.get("id");RoleWorkspaceDTO command=new RoleWorkspaceDTO();command.setRoleId("id");command.setRevision(current.getRevision());
        command.setSkipUrl("//external");assertThrows(BusinessException.class,()->service.saveDefault(command));
        command.setSkipUrl("/not-granted");assertThrows(BusinessException.class,()->service.saveDefault(command));
        verify(dao,never()).updateDefault(any(),anyString());
        command.setSkipUrl("/allowed");service.saveDefault(command);verify(dao).updateDefault(role,"/allowed");
    }
    @Test void cacheRefreshWaitsForCommit() {
        allow();var current=service.get("id");RoleWorkspaceDTO command=new RoleWorkspaceDTO();command.setRoleId("id");command.setRevision(current.getRevision());command.setFuncCodes(List.of("VALID"));command.setResourceKeys(List.of());
        TransactionSynchronizationManager.initSynchronization();service.saveGrants(command);verifyNoInteractions(refresh);
        TransactionSynchronizationManager.getSynchronizations().forEach(callback->callback.afterCommit());
        verify(refresh).refreshUsersByRole("tenant","org","ROLE",2026);
    }
    @Test void loginDefaultIsOnlyUsedForASingleRoleAndAnAuthorisedLocalPage() {
        var snapshot=new LoginPermissionSnapshotVO();var one=new RoleVO();one.setSkipUrl("/allowed");var menu=new MenuVO();menu.setMenuUrl("/allowed");var route=new RouterVO();route.setRouterUrl("/allowed");
        snapshot.setRoleList(List.of(one));snapshot.setMenuList(List.of(menu));snapshot.setRouterList(List.of(route));assertEquals("/allowed",RoleDefaultRouteSupport.resolve(snapshot));
        snapshot.setRoleList(List.of(one,new RoleVO()));assertEquals("/workspace",RoleDefaultRouteSupport.resolve(snapshot));
        snapshot.setRoleList(List.of(one));snapshot.setRouterList(List.of());assertEquals("/workspace",RoleDefaultRouteSupport.resolve(snapshot));
        one.setSkipUrl("//external");assertEquals("/workspace",RoleDefaultRouteSupport.resolve(snapshot));
    }
}
