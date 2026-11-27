package com.peach.auth.service.assembler;

import com.peach.auth.dao.AuthFunctionDao;
import com.peach.auth.dao.AuthResourceDao;
import com.peach.auth.dao.MenuDao;
import com.peach.auth.dao.RouterDao;
import com.peach.auth.vo.AuthFunctionVO;
import com.peach.auth.vo.AuthResourceVO;
import com.peach.auth.vo.LoginPermissionSnapshotVO;
import com.peach.auth.vo.MenuVO;
import com.peach.auth.vo.RoleVO;
import com.peach.auth.vo.RouterVO;
import com.peach.auth.vo.UserOrgVO;
import com.peach.common.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 登录权限快照装配器，统一编译角色、功能、菜单、路由与资源授权结果。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 20:00
 */
@Indexed
@Service
@RequiredArgsConstructor
public class LoginPermissionAssembler {

    private final AuthFunctionDao authFunctionDao;
    private final AuthResourceDao authResourceDao;
    private final MenuDao menuDao;
    private final RouterDao routerDao;

    public LoginPermissionSnapshotVO assemble(UserOrgVO currentUserOrg,
                                              List<RoleVO> roleList) {
        LoginPermissionSnapshotVO snapshot = new LoginPermissionSnapshotVO();
        snapshot.setRoleList(roleList == null ? List.of() : roleList);

        Set<String> roleCodes = collectRoleCodes(roleList);

        if (roleCodes.isEmpty()) {
            snapshot.setMenuList(List.of());
            snapshot.setRouterList(List.of());
            snapshot.setResourceList(List.of());
            snapshot.setPermissionList(List.of());
            snapshot.setApiResourceCodes(List.of());
            snapshot.setButtonResourceCodes(List.of());
            return snapshot;
        }

        String tenantId = currentUserOrg.getTenantId();
        String orgId = currentUserOrg.getOrgId();
        Set<String> funcCodes = loadAuthorizedFunctionCodes(tenantId, orgId, roleCodes);
        List<AuthResourceVO> resources = loadAuthorizedResources(tenantId, orgId, roleCodes);
        List<MenuVO> menus = loadMenus(tenantId, funcCodes);
        List<RouterVO> routers = loadRouters(tenantId, funcCodes);

        snapshot.setResourceList(resources);
        snapshot.setMenuList(menus);
        snapshot.setRouterList(routers);
        snapshot.setPermissionList(buildPermissionList(resources));
        snapshot.setApiResourceCodes(extractResourceCodes(resources, "API"));
        snapshot.setButtonResourceCodes(extractResourceCodes(resources, "BUTTON"));
        return snapshot;
    }

    private Set<String> collectRoleCodes(List<RoleVO> roleList) {
        if (CollectionUtils.isEmpty(roleList)) {
            return Set.of();
        }
        return roleList.stream()
                .map(RoleVO::getRoleCode)
                .filter(StringUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> loadAuthorizedFunctionCodes(String tenantId,
                                                    String orgId,
                                                    Set<String> roleCodes) {
        if (roleCodes.isEmpty()) {
            return Set.of();
        }
        List<AuthFunctionVO> authFunctions = authFunctionDao.selectByPartyCodes(
                tenantId, orgId, roleCodes);
        if (CollectionUtils.isEmpty(authFunctions)) {
            return Set.of();
        }
        return authFunctions.stream()
                .map(AuthFunctionVO::getFuncCode)
                .filter(StringUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<AuthResourceVO> loadAuthorizedResources(String tenantId,
                                                         String orgId,
                                                         Set<String> roleCodes) {
        if (roleCodes.isEmpty()) {
            return List.of();
        }
        List<AuthResourceVO> resources = authResourceDao.selectByPartyCodes(
                tenantId, orgId, roleCodes);
        if (CollectionUtils.isEmpty(resources)) {
            return List.of();
        }
        Map<String, AuthResourceVO> resourceMap = new LinkedHashMap<>();
        for (AuthResourceVO resource : resources) {
            if (resource == null || StringUtil.isBlank(resource.getResourceCode())) {
                continue;
            }
            String key = resource.getOpType() + ":" + resource.getResourceCode();
            resourceMap.put(key, resource);
        }
        return new ArrayList<>(resourceMap.values());
    }

    private List<MenuVO> loadMenus(String tenantId, Set<String> funcCodes) {
        if (funcCodes.isEmpty()) {
            return List.of();
        }
        List<MenuVO> menus = menuDao.selectByFuncCodes(tenantId, funcCodes);
        if (CollectionUtils.isEmpty(menus)) {
            return List.of();
        }
        Map<String, MenuVO> result = new LinkedHashMap<>();
        for (MenuVO menu : menus) { if (menu != null && StringUtil.isNotBlank(menu.getMenuId())) result.put(menu.getMenuId(), menu); }
        Map<String, MenuVO> catalogs = new LinkedHashMap<>();
        List<MenuVO> available = menuDao.selectCatalogs(tenantId);
        if (available != null) for (MenuVO catalog : available) catalogs.put(catalog.getMenuId(), catalog);
        for (MenuVO menu : menus) {
            if (menu == null) continue;
            String parent = menu.getParentMenuId();
            Set<String> visited = new LinkedHashSet<>();
            while (StringUtil.isNotBlank(parent) && visited.add(parent)) {
                MenuVO catalog = catalogs.get(parent);
                if (catalog == null) break;
                result.putIfAbsent(parent, catalog);
                parent = catalog.getParentMenuId();
            }
        }
        return new ArrayList<>(result.values());
    }

    private List<RouterVO> loadRouters(String tenantId, Set<String> funcCodes) {
        Map<String, RouterVO> routerMap = new LinkedHashMap<>();
        List<RouterVO> publicRouters = routerDao.selectPublicRouters(tenantId);
        if (!CollectionUtils.isEmpty(publicRouters)) {
            for (RouterVO router : publicRouters) {
                if (router != null && StringUtil.isNotBlank(router.getRouterId())) {
                    routerMap.put(router.getRouterId(), router);
                }
            }
        }
        if (!funcCodes.isEmpty()) {
            List<RouterVO> authorizedRouters = routerDao.selectByFuncCodes(tenantId, funcCodes);
            if (!CollectionUtils.isEmpty(authorizedRouters)) {
                for (RouterVO router : authorizedRouters) {
                    if (router != null && StringUtil.isNotBlank(router.getRouterId())) {
                        routerMap.put(router.getRouterId(), router);
                    }
                }
            }
        }
        return new ArrayList<>(routerMap.values());
    }

    private List<String> buildPermissionList(List<AuthResourceVO> resourceList) {
        if (CollectionUtils.isEmpty(resourceList)) {
            return List.of();
        }
        Set<String> permissionSet = new LinkedHashSet<>();
        for (AuthResourceVO resource : resourceList) {
            if (resource != null && StringUtil.isNotBlank(resource.getResourceCode())) {
                permissionSet.add(resource.getResourceCode());
            }
        }
        return new ArrayList<>(permissionSet);
    }

    private List<String> extractResourceCodes(List<AuthResourceVO> resourceList, String opType) {
        if (CollectionUtils.isEmpty(resourceList) || StringUtil.isBlank(opType)) {
            return List.of();
        }
        Set<String> codes = new LinkedHashSet<>();
        for (AuthResourceVO resource : resourceList) {
            if (resource == null || StringUtil.isBlank(resource.getResourceCode())) {
                continue;
            }
            if (opType.equalsIgnoreCase(resource.getOpType())) {
                codes.add(resource.getResourceCode());
            }
        }
        return new ArrayList<>(codes);
    }
}
