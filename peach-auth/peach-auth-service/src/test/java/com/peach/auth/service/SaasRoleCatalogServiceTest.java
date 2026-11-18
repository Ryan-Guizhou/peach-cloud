package com.peach.auth.service;

import com.peach.auth.dao.SaasRoleCatalogDao;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.auth.vo.SaasCatalogEntryVO;
import com.peach.auth.vo.SaasCatalogPageVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 只允许真实当前年度的租户管理成员查询最小可委派目录。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 15:35
 */
class SaasRoleCatalogServiceTest {

    private static final String ROLE_ID = "1234567890abcdef1234567890abcdef";

    private final TenantSessionContext context =
            new TenantSessionContext("user", "tenant", "member", "app", 2026, 2, 7);

    private TenantSessionService sessionService;
    private ScopedPermissionService permissionService;
    private SaasRoleCatalogDao dao;
    private SaasRoleCatalogService catalog;

    @BeforeEach
    void setUp() {
        sessionService = mock(TenantSessionService.class);
        permissionService = mock(ScopedPermissionService.class);
        dao = mock(SaasRoleCatalogDao.class);
        catalog = new SaasRoleCatalogService(sessionService, permissionService, dao);
        when(sessionService.requireCurrent()).thenReturn(context);
    }

    @Test
    void shouldRejectUnprivilegedMemberBeforeQueryingAnyDirectory() {
        assertThatThrownBy(() -> catalog.roles(1, 20, ""))
                .isInstanceOf(SecurityException.class);
        verifyNoInteractions(dao);
    }

    @Test
    void shouldBoundRolePagesAndUseOnlyCurrentTenantAppYear() {
        allowManage();
        SaasCatalogEntryVO one = entry("one");
        SaasCatalogEntryVO two = entry("two");
        SaasCatalogEntryVO sentinel = entry("three");
        when(dao.selectRoles("tenant", "app", 2026, "", 3, 0))
                .thenReturn(List.of(one, two, sentinel));

        SaasCatalogPageVO page = catalog.roles(1, 2, null);

        assertThat(page.getItems()).containsExactly(one, two);
        assertThat(page.isHasMore()).isTrue();
        assertThat(page.getPage()).isEqualTo(1);
        verify(dao).selectRoles("tenant", "app", 2026, "", 3, 0);
    }

    @Test
    void shouldQueryOnlyAdministratorHeldDelegablePermissions() {
        allowManage();
        when(dao.selectDelegablePermissions("tenant", "app", 2026, "member", "ROLE", 21, 0))
                .thenReturn(List.of(entry("permission")));

        SaasCatalogPageVO page = catalog.permissions(1, 20, "ROLE");

        assertThat(page.getItems()).hasSize(1);
        verify(dao).selectDelegablePermissions("tenant", "app", 2026, "member", "ROLE", 21, 0);
    }

    @Test
    void shouldRejectInvalidPagingAndOversizedSearch() {
        allowManage();
        assertThatThrownBy(() -> catalog.members(0, 20, ""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> catalog.organizations(1, 51, ""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> catalog.permissions(1, 20, "a".repeat(65)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(dao);
    }

    @Test
    void shouldFailClosedOnTruncatedRolePermissionList() {
        allowManage();
        when(dao.selectRolePermissionRefs("tenant", "app", 2026, ROLE_ID))
                .thenReturn(Collections.nCopies(201, ROLE_ID));

        assertThatThrownBy(() -> catalog.rolePermissionRefs(ROLE_ID))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void shouldNotReadBindingsForUntrustedPhysicalId() {
        allowManage();
        assertThatThrownBy(() -> catalog.memberBindings("../member"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(dao);
    }

    private void allowManage() {
        when(permissionService.hasTenantPermission("tenant", "member", "app",
                2026, "TENANT.ROLE.MANAGE")).thenReturn(true);
    }

    private SaasCatalogEntryVO entry(String id) {
        SaasCatalogEntryVO row = new SaasCatalogEntryVO();
        row.setId(id);
        return row;
    }
}
