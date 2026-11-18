package com.peach.auth.service;

import com.peach.auth.dao.SaasLoginSnapshotDao;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.auth.vo.LoginPermissionSnapshotVO;
import com.peach.auth.vo.MenuVO;
import com.peach.auth.vo.RoleVO;
import com.peach.auth.vo.RouterVO;
import com.peach.auth.vo.SaasLoginGrantRowVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 按已验证的 Token SaaS 成员上下文装配登录授权快照。
 *
 * <p>权限事实源仅是当前数据库中同一有效成员角色绑定和角色资源授权。
 * MENU 到路由必须有物理 PERMISSION_REF_ID 对应的受控 UI 目录记录；
 * 绝不从旧机构 FISCAL、业务角色 CODE、旧缓存或静态本地菜单推断权限。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 15:10
 */
@Service
@RequiredArgsConstructor
public class SaasLoginSnapshotService {

    private static final int MAX_GRANT_ROWS = 1024;
    private static final Pattern ROUTE = Pattern.compile("^/[a-z0-9-]+(?:/[a-z0-9-]+)*$");
    private static final Pattern COMPONENT = Pattern.compile("^[a-z0-9_-]+(?:/[a-z0-9_-]+)*$");
    private static final Pattern UI_CODE = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{0,63}$");

    private final TenantSessionService tenantSessionService;
    private final SaasLoginSnapshotDao snapshotDao;

    /**
     * 为当前真实成员生成有界授权快照；无法保证完整性时拒绝，而非返回更多权限。
     *
     * @param expected 登录或切换租户后已绑定的物理身份
     * @return 只包含真实有效的当前角色、资源及显式登记的菜单/路由
     */
    @Transactional(readOnly = true)
    public LoginPermissionSnapshotVO requireSnapshot(TenantSessionContext expected) {
        if (expected == null || !expected.equals(tenantSessionService.requireCurrent())) {
            throw new SecurityException("Trusted tenant member context has changed");
        }
        List<SaasLoginGrantRowVO> rows = snapshotDao.selectCurrentGrants(
                expected.userRefId(), expected.tenantRefId(), expected.memberRefId(),
                expected.appRefId(), expected.memberVersion(), expected.authVersion());
        if (rows != null && rows.size() > MAX_GRANT_ROWS) {
            throw new SecurityException("Login permission catalog exceeds safe limit");
        }

        Map<String, RoleVO> roles = new LinkedHashMap<>();
        Map<String, MenuVO> menus = new LinkedHashMap<>();
        Map<String, RouterVO> routers = new LinkedHashMap<>();
        Map<String, String> routeOwners = new LinkedHashMap<>();
        Set<String> permissions = new LinkedHashSet<>();
        Set<String> apiCodes = new LinkedHashSet<>();
        Set<String> buttonCodes = new LinkedHashSet<>();

        if (!CollectionUtils.isEmpty(rows)) {
            for (SaasLoginGrantRowVO row : rows) {
                if (row == null || !StringUtils.hasText(row.getRoleRefId())
                        || !StringUtils.hasText(row.getPermissionRefId())
                        || !StringUtils.hasText(row.getPermissionCode())) {
                    throw new SecurityException("Incomplete current role authorization");
                }
                roles.computeIfAbsent(row.getRoleRefId(), key -> roleView(row, expected));
                permissions.add(row.getPermissionCode());
                if ("API".equals(row.getResourceType())) {
                    apiCodes.add(row.getPermissionCode());
                } else if ("BUTTON".equals(row.getResourceType())) {
                    buttonCodes.add(row.getPermissionCode());
                } else if ("MENU".equals(row.getResourceType())
                        && StringUtils.hasText(row.getUiRefId())) {
                    addUiRoute(row, menus, routers, routeOwners);
                }
            }
        }

        LoginPermissionSnapshotVO result = new LoginPermissionSnapshotVO();
        result.setRoleList(new ArrayList<>(roles.values()));
        result.setMenuList(new ArrayList<>(menus.values()));
        result.setRouterList(new ArrayList<>(routers.values()));
        result.setResourceList(List.of());
        result.setPermissionList(new ArrayList<>(permissions));
        result.setApiResourceCodes(new ArrayList<>(apiCodes));
        result.setButtonResourceCodes(new ArrayList<>(buttonCodes));
        return result;
    }

    private RoleVO roleView(SaasLoginGrantRowVO row, TenantSessionContext context) {
        RoleVO role = new RoleVO();
        role.setId(row.getRoleRefId());
        role.setRoleId(row.getRoleId());
        role.setRoleCode(row.getRoleCode());
        role.setRoleName(row.getRoleName());
        role.setRoleScope(row.getScopeType());
        role.setTenantRefId(context.tenantRefId());
        role.setAppRefId(context.appRefId());
        return role;
    }

    private void addUiRoute(SaasLoginGrantRowVO row, Map<String, MenuVO> menus,
                            Map<String, RouterVO> routers, Map<String, String> routeOwners) {
        if (!StringUtils.hasText(row.getPermissionId())
                || !StringUtils.hasText(row.getMenuName())
                || !valid(row.getMenuCode(), UI_CODE)
                || !valid(row.getRouterCode(), UI_CODE)
                || !valid(row.getRouterUrl(), ROUTE)
                || !valid(row.getFilePath(), COMPONENT)) {
            throw new SecurityException("Invalid trusted UI permission mapping");
        }
        String prior = routeOwners.putIfAbsent(row.getRouterUrl(), row.getPermissionRefId());
        if (prior != null && !prior.equals(row.getPermissionRefId())) {
            throw new SecurityException("Ambiguous UI route permission mapping");
        }
        MenuVO menu = new MenuVO();
        menu.setId(row.getUiRefId());
        menu.setMenuId(row.getPermissionId());
        menu.setMenuCode(row.getMenuCode());
        menu.setMenuName(row.getMenuName());
        menu.setMenuUrl(row.getRouterUrl());
        menu.setIsLeaf(1);
        menu.setIsShow(1);
        menu.setIsDisable(0);
        menu.setMenuIcon(row.getMenuIcon());
        menus.putIfAbsent(row.getUiRefId(), menu);

        RouterVO router = new RouterVO();
        router.setId(row.getUiRefId());
        router.setRouterId(row.getPermissionId());
        router.setRouterCode(row.getRouterCode());
        router.setRouterName(row.getMenuName());
        router.setRouterUrl(row.getRouterUrl());
        router.setFilePath(row.getFilePath());
        router.setIsAuth(1);
        routers.putIfAbsent(row.getUiRefId(), router);
    }

    private boolean valid(String value, Pattern pattern) {
        return StringUtils.hasText(value) && pattern.matcher(value).matches();
    }
}
