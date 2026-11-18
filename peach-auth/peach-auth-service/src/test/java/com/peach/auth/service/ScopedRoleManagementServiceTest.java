package com.peach.auth.service;

import com.peach.auth.dao.IamAuthorizationAuditDao;
import com.peach.auth.dao.MemberAuthVersionDao;
import com.peach.auth.dao.OrganizationHierarchyDao;
import com.peach.auth.dao.ScopedRoleManagementDao;
import com.peach.auth.dao.ScopedDataPolicyManagementDao;
import com.peach.auth.entity.RoleDO;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.auth.service.support.AuthBusinessIdGenerator;
import com.peach.auth.service.support.AuthBusinessIdType;
import com.peach.auth.vo.RolePermissionGrantVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 校验角色创建、权限差量同步、不可委派权限和授权版本变更。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 18:25
 */
class ScopedRoleManagementServiceTest {

    private ScopedRoleManagementDao roleDao;
    private MemberAuthVersionDao versionDao;
    private ScopedDataPolicyManagementDao dataPolicyManagementDao;
    private IamAuthorizationAuditDao auditDao;
    private ScopedPermissionService permissionService;
    private AuthBusinessIdGenerator businessIdGenerator;
    private ScopedRoleManagementService service;

    @BeforeEach
    void prepare() {
        TenantMembershipPolicy policy = mock(TenantMembershipPolicy.class);
        TenantSessionService sessionService = mock(TenantSessionService.class);
        roleDao = mock(ScopedRoleManagementDao.class);
        dataPolicyManagementDao = mock(ScopedDataPolicyManagementDao.class);
        versionDao = mock(MemberAuthVersionDao.class);
        auditDao = mock(IamAuthorizationAuditDao.class);
        permissionService = mock(ScopedPermissionService.class);
        businessIdGenerator = mock(AuthBusinessIdGenerator.class);
        OrganizationHierarchyDao hierarchyDao = mock(OrganizationHierarchyDao.class);
        when(sessionService.requireCurrent()).thenReturn(
                new TenantSessionContext("user", "tenant", "admin", "app", 1L, 2L));
        when(hierarchyDao.lockTenantAndFindBusinessId("tenant")).thenReturn("TENANT_BIZ");
        service = new ScopedRoleManagementService(policy, sessionService, roleDao,
                dataPolicyManagementDao, hierarchyDao, permissionService, versionDao, auditDao, businessIdGenerator);
    }

    @Test
    void shouldCreateTenantRoleWithinCurrentApp() {
        when(businessIdGenerator.next(AuthBusinessIdType.ROLE)).thenReturn("ROLE_00000001");
        when(roleDao.insertRole(any())).thenReturn(1);
        when(auditDao.append(anyString(), eq("tenant"), eq("app"),
                eq("admin"), anyString(), eq("ROLE_CREATE"), eq(0L), eq(0L))).thenReturn(1);

        String id = service.createRole("tenant", "admin", "ROLE_EDITOR", "Editor");

        assertThat(id).isNotBlank();
        verify(roleDao).insertRole(any());
        verify(auditDao).append(anyString(), eq("tenant"), eq("app"),
                eq("admin"), eq(id), eq("ROLE_CREATE"), eq(0L), eq(0L));
    }

    @Test
    void shouldApplyPermissionDiffAndInvalidateAllOldTenantGrants() {
        RoleDO role = role("TENANT");
        when(roleDao.lockScopedRole("tenant", "app", "role")).thenReturn(role);
        when(roleDao.selectActivePermissionRefs("tenant", "role")).thenReturn(List.of("permission-1"));
        when(roleDao.selectDelegablePermissions(eq("tenant"), eq("app"), any()))
                .thenReturn(List.of(permission("permission-1", "DOC.VIEW"),
                        permission("permission-2", "DOC.WRITE")));
        when(permissionService.hasTenantPermission("tenant", "admin", "app",
                "DOC.VIEW")).thenReturn(true);
        when(permissionService.hasTenantPermission("tenant", "admin", "app",
                "DOC.WRITE")).thenReturn(true);
        when(roleDao.insertRolePermission(anyString(), eq("tenant"), eq("role"),
                eq("permission-2"), eq("admin"))).thenReturn(1);
        when(roleDao.incrementRoleVersion("tenant", "app", "role", 3L, "admin"))
                .thenReturn(1);
        when(versionDao.incrementForTenant("tenant")).thenReturn(2);
        when(auditDao.append(anyString(), eq("tenant"), eq("app"),
                eq("admin"), eq("role"), eq("ROLE_PERMISSION_SYNC"), eq(3L), eq(4L)))
                .thenReturn(1);

        service.replacePermissions("tenant", "admin", "role", List.of("permission-1", "permission-2"));

        verify(roleDao).insertRolePermission(anyString(), eq("tenant"), eq("role"),
                eq("permission-2"), eq("admin"));
        verify(versionDao).incrementForTenant("tenant");
        verify(auditDao).append(anyString(), eq("tenant"), eq("app"),
                eq("admin"), eq("role"), eq("ROLE_PERMISSION_SYNC"), eq(3L), eq(4L));
    }

    @Test
    void shouldNeverAllowTenantAdminToMutateSystemRole() {
        when(roleDao.lockScopedRole("tenant", "app", "role"))
                .thenReturn(role("SYSTEM"));
        assertThatThrownBy(() -> service.replacePermissions(
                "tenant", "admin", "role", List.of("permission-1")))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("platform");
        verify(roleDao, never()).insertRolePermission(
                anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void shouldRejectNonDelegablePermission() {
        when(roleDao.lockScopedRole("tenant", "app", "role"))
                .thenReturn(role("TENANT"));
        when(roleDao.selectActivePermissionRefs("tenant", "role")).thenReturn(List.of());
        when(roleDao.selectDelegablePermissions("tenant", "app", List.of("nondelegable")))
                .thenReturn(List.of());
        assertThatThrownBy(() -> service.replacePermissions(
                "tenant", "admin", "role", List.of("nondelegable")))
                .isInstanceOf(SecurityException.class);
        verify(versionDao, never()).incrementForTenant(anyString());
    }

    @Test
    void shouldNotAcceptDuplicateIdsOrCrossYearRoles() {
        when(roleDao.lockScopedRole("tenant", "app", "role"))
                .thenReturn(role("TENANT"));
        assertThatThrownBy(() -> service.replacePermissions(
                "tenant", "admin", "role", List.of("same", "same")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.replacePermissions(
                "tenant", "admin", "another-role", List.of("permission")))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void shouldRetirePriorFieldAndDataPoliciesOnPermissionRevoke() {
        when(roleDao.lockScopedRole("tenant", "app", "role"))
                .thenReturn(role("TENANT"));
        when(roleDao.selectActivePermissionRefs("tenant", "role")).thenReturn(List.of("old"));
        when(roleDao.revokeRolePermission("tenant", "role", "old")).thenReturn(1);
        when(roleDao.incrementRoleVersion("tenant", "app", "role", 3L, "admin"))
                .thenReturn(1);
        when(versionDao.incrementForTenant("tenant")).thenReturn(1);
        when(auditDao.append(anyString(), eq("tenant"), eq("app"),
                eq("admin"), eq("role"), eq("ROLE_PERMISSION_SYNC"), eq(3L), eq(4L)))
                .thenReturn(1);
        service.replacePermissions("tenant", "admin", "role", List.of());
        verify(dataPolicyManagementDao).retireDataPolicies("tenant", "app", "role", "old");
        verify(dataPolicyManagementDao).retireFieldPolicies("tenant", "app", "role", "old");
    }

    private static RoleDO role(String roleType) {
        RoleDO role = new RoleDO();
        role.setId("role");
        role.setRoleType(roleType);
        role.setVersion(3L);
        return role;
    }

    private static RolePermissionGrantVO permission(String id, String code) {
        RolePermissionGrantVO result = new RolePermissionGrantVO();
        result.setPermissionRefId(id);
        result.setPermissionCode(code);
        return result;
    }
}
