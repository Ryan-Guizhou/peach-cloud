package com.peach.auth.service;

import com.peach.auth.dao.SaasLoginSnapshotDao;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.auth.vo.LoginPermissionSnapshotVO;
import com.peach.auth.vo.SaasLoginGrantRowVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 登录授权快照的受信物理身份、范围、路由目录和超量拒绝回归。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 15:15
 */
class SaasLoginSnapshotServiceTest {

    private final TenantSessionContext current =
            new TenantSessionContext("user-ref", "tenant-ref", "member-ref", "app-ref", 2026, 2, 7);

    private TenantSessionService sessionService;
    private SaasLoginSnapshotDao dao;
    private SaasLoginSnapshotService service;

    @BeforeEach
    void setUp() {
        sessionService = mock(TenantSessionService.class);
        dao = mock(SaasLoginSnapshotDao.class);
        service = new SaasLoginSnapshotService(sessionService, dao);
        when(sessionService.requireCurrent()).thenReturn(current);
    }

    @Test
    void shouldReturnOnlyAuthoritativelyGrantedMenuAndActualButtonApiCodes() {
        SaasLoginGrantRowVO menu = granted("MENU.WORKSPACE", "MENU", "perm-menu");
        menu.setPermissionId("perm-biz-workspace");
        menu.setUiRefId("ui-ref");
        menu.setMenuCode("workspace");
        menu.setMenuName("工作台");
        menu.setRouterCode("SYS_WORKSPACE");
        menu.setRouterUrl("/workspace");
        menu.setFilePath("workspace/overview");
        SaasLoginGrantRowVO button = granted("TENANT.ROLE.MANAGE", "BUTTON", "perm-button");
        SaasLoginGrantRowVO api = granted("POST:/api/auth/saas/roles", "API", "perm-api");
        whenCurrentGrants(List.of(menu, button, api, menu));

        LoginPermissionSnapshotVO result = service.requireSnapshot(current);

        assertThat(result.getRoleList()).hasSize(1);
        assertThat(result.getRoleList().get(0).getId()).isEqualTo("role-ref");
        assertThat(result.getRoleList().get(0).getAuthYear()).isEqualTo(2026);
        assertThat(result.getMenuList()).hasSize(1);
        assertThat(result.getMenuList().get(0).getMenuCode()).isEqualTo("workspace");
        assertThat(result.getRouterList()).hasSize(1);
        assertThat(result.getRouterList().get(0).getRouterUrl()).isEqualTo("/workspace");
        assertThat(result.getPermissionList()).containsExactly(
                "MENU.WORKSPACE", "TENANT.ROLE.MANAGE", "POST:/api/auth/saas/roles");
        assertThat(result.getApiResourceCodes()).containsExactly("POST:/api/auth/saas/roles");
        assertThat(result.getButtonResourceCodes()).containsExactly("TENANT.ROLE.MANAGE");
        assertThat(result.getResourceList()).isEmpty();
        verify(dao).selectCurrentGrants("user-ref", "tenant-ref", "member-ref",
                "app-ref", 2026, 2, 7);
    }

    @Test
    void shouldDenyChangedTenantOrYearBeforeReadingAnyGrants() {
        TenantSessionContext mismatched =
                new TenantSessionContext("user-ref", "tenant-other", "member-ref", "app-ref", 2027, 2, 7);
        assertThatThrownBy(() -> service.requireSnapshot(mismatched))
                .isInstanceOf(SecurityException.class);
        verify(dao, never()).selectCurrentGrants(
                "user-ref", "tenant-other", "member-ref", "app-ref", 2027, 2, 7);
    }

    @Test
    void shouldUseEmptyAuthorizationForMemberWithoutEffectiveGrants() {
        whenCurrentGrants(List.of());

        LoginPermissionSnapshotVO result = service.requireSnapshot(current);

        assertThat(result.getRoleList()).isEmpty();
        assertThat(result.getMenuList()).isEmpty();
        assertThat(result.getRouterList()).isEmpty();
        assertThat(result.getPermissionList()).isEmpty();
    }

    @Test
    void shouldNotInventMenuFromPermissionWithoutTrustedRouteMapping() {
        whenCurrentGrants(List.of(granted("MENU.WORKSPACE", "MENU", "perm-menu")));

        LoginPermissionSnapshotVO result = service.requireSnapshot(current);

        assertThat(result.getPermissionList()).containsExactly("MENU.WORKSPACE");
        assertThat(result.getMenuList()).isEmpty();
        assertThat(result.getRouterList()).isEmpty();
    }

    @Test
    void shouldRejectTwoPermissionsTryingToOwnSameFrontendPath() {
        SaasLoginGrantRowVO one = mappedMenu("perm-one", "ui-one", "/workspace");
        SaasLoginGrantRowVO two = mappedMenu("perm-two", "ui-two", "/workspace");
        whenCurrentGrants(List.of(one, two));

        assertThatThrownBy(() -> service.requireSnapshot(current))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Ambiguous");
    }

    @Test
    void shouldRejectUnsafeMappedFileOrRoute() {
        SaasLoginGrantRowVO row = mappedMenu("perm-one", "ui-one", "/workspace");
        row.setFilePath("../external");
        whenCurrentGrants(List.of(row));

        assertThatThrownBy(() -> service.requireSnapshot(current))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Invalid");
    }

    @Test
    void shouldFailClosedOnTruncatedAuthorizationCatalog() {
        whenCurrentGrants(Collections.nCopies(1025, granted("TENANT.ROLE.MANAGE", "API", "p")));

        assertThatThrownBy(() -> service.requireSnapshot(current))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("safe limit");
    }

    private void whenCurrentGrants(List<SaasLoginGrantRowVO> rows) {
        when(dao.selectCurrentGrants("user-ref", "tenant-ref", "member-ref",
                "app-ref", 2026, 2, 7)).thenReturn(rows);
    }

    private SaasLoginGrantRowVO granted(String code, String type, String permissionRefId) {
        SaasLoginGrantRowVO row = new SaasLoginGrantRowVO();
        row.setRoleRefId("role-ref");
        row.setRoleId("role-biz");
        row.setRoleCode("ROLE_TENANT_ADMIN");
        row.setRoleName("租户管理员");
        row.setScopeType("TENANT");
        row.setPermissionCode(code);
        row.setPermissionRefId(permissionRefId);
        row.setResourceType(type);
        return row;
    }

    private SaasLoginGrantRowVO mappedMenu(String permissionRefId, String uiRefId, String url) {
        SaasLoginGrantRowVO row = granted("MENU.WORKSPACE", "MENU", permissionRefId);
        row.setPermissionId(permissionRefId);
        row.setUiRefId(uiRefId);
        row.setMenuCode("workspace");
        row.setMenuName("工作台");
        row.setRouterCode("SYS_WORKSPACE");
        row.setRouterUrl(url);
        row.setFilePath("workspace/overview");
        return row;
    }
}
