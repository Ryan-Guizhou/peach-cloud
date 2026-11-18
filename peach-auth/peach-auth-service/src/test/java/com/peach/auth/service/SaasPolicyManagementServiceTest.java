package com.peach.auth.service;

import com.peach.auth.dao.IamAuthorizationAuditDao;
import com.peach.auth.dao.MemberAuthVersionDao;
import com.peach.auth.dao.OrganizationHierarchyDao;
import com.peach.auth.dao.ScopedDataAuthorizationDao;
import com.peach.auth.dao.ScopedDataPolicyManagementDao;
import com.peach.auth.dao.ScopedRoleManagementDao;
import com.peach.auth.dto.SaasDataPolicyUpdateDTO;
import com.peach.auth.dto.SaasFieldPolicyUpdateDTO;
import com.peach.auth.entity.RoleDO;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.auth.vo.RoleDataGrantVO;
import com.peach.auth.vo.RolePermissionGrantVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证策略更新的权限边界、不可委派系统动作、历史策略撤销与审计失效。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 11:05
 */
class SaasPolicyManagementServiceTest {

    private ScopedRoleManagementDao roleDao;
    private ScopedDataAuthorizationDao dataAuthorizationDao;
    private ScopedDataPolicyManagementDao policyDao;
    private MemberAuthVersionDao versionDao;
    private IamAuthorizationAuditDao auditDao;
    private ScopedPermissionService permissionService;
    private SaasPolicyManagementService service;

    @BeforeEach
    void setUp() {
        TenantSessionService session = mock(TenantSessionService.class);
        TenantMembershipPolicy membership = mock(TenantMembershipPolicy.class);
        roleDao = mock(ScopedRoleManagementDao.class);
        dataAuthorizationDao = mock(ScopedDataAuthorizationDao.class);
        policyDao = mock(ScopedDataPolicyManagementDao.class);
        permissionService = mock(ScopedPermissionService.class);
        OrganizationHierarchyDao hierarchy = mock(OrganizationHierarchyDao.class);
        versionDao = mock(MemberAuthVersionDao.class);
        auditDao = mock(IamAuthorizationAuditDao.class);
        service = new SaasPolicyManagementService(
                session, membership, roleDao, dataAuthorizationDao, policyDao,
                permissionService, hierarchy, versionDao, auditDao);

        when(session.requireCurrent()).thenReturn(
                new TenantSessionContext("user", "tenant", "admin", "app", 3L, 2L));
        when(hierarchy.lockTenantAndFindBusinessId("tenant")).thenReturn("TENANT_BIZ");
        RoleDO role = new RoleDO();
        role.setId("role");
        role.setRoleType("TENANT");
        role.setVersion(5L);
        when(roleDao.lockScopedRole("tenant", "app", "role")).thenReturn(role);
        when(roleDao.selectActivePermissionRefs("tenant", "role")).thenReturn(List.of("permission"));
        when(roleDao.selectDelegablePermissions("tenant", "app", List.of("permission")))
                .thenReturn(List.of(permission("TENANT")));
        when(permissionService.hasTenantPermission("tenant", "admin", "app",
                "DOC.READ")).thenReturn(true);
    }

    @Test
    void shouldReplaceDataPolicyAndInvalidateMemberTokensWithAudit() {
        when(policyDao.retireDataPolicies("tenant", "app", "role", "permission")).thenReturn(0);
        when(policyDao.insertDataPolicy(anyString(), eq("tenant"), eq("app"), eq("role"),
                eq("permission"), eq("SELF"), eq(null), eq("admin"))).thenReturn(1);
        allowVersionAndAudit("DATA_POLICY_REPLACE");

        service.replaceDataPolicies("role", "permission", List.of(dataRule("SELF", null)));

        verify(policyDao).insertDataPolicy(anyString(), eq("tenant"), eq("app"),
                eq("role"), eq("permission"), eq("SELF"), eq(null), eq("admin"));
        verify(versionDao).incrementForTenant("tenant");
        verify(auditDao).append(anyString(), eq("tenant"), eq("app"),
                eq("admin"), eq("role"), eq("DATA_POLICY_REPLACE"), eq(5L), eq(6L));
    }

    @Test
    void shouldDenySystemActionWithoutFullScopeProof() {
        when(roleDao.selectDelegablePermissions("tenant", "app", List.of("permission")))
                .thenReturn(List.of(permission("SYSTEM")));
        assertThatThrownBy(() -> service.replaceDataPolicies(
                "role", "permission", List.of(dataRule("TENANT_ALL", null))))
                .isInstanceOf(SecurityException.class);
        verify(policyDao, never()).retireDataPolicies(
                anyString(), anyString(), anyString(), anyString());

        RoleDataGrantVO broad = new RoleDataGrantVO();
        broad.setBindingRefId("binding");
        broad.setRoleRefId("admin-role");
        broad.setPermissionRefId("permission");
        broad.setBindingScopeType("TENANT");
        broad.setDataScope("TENANT_ALL");
        when(dataAuthorizationDao.selectDataGrants("tenant", "admin", "app", "DOC.READ"))
                .thenReturn(List.of(broad));
        when(policyDao.retireDataPolicies("tenant", "app", "role", "permission")).thenReturn(0);
        when(policyDao.insertDataPolicy(anyString(), eq("tenant"), eq("app"), eq("role"),
                eq("permission"), eq("TENANT_ALL"), eq(null), eq("admin"))).thenReturn(1);
        allowVersionAndAudit("DATA_POLICY_REPLACE");

        service.replaceDataPolicies("role", "permission", List.of(dataRule("TENANT_ALL", null)));
        verify(versionDao).incrementForTenant("tenant");
    }

    @Test
    void shouldRejectForeignOrUnavailableFieldsWithoutPartialWrite() {
        when(policyDao.selectDelegableFieldRefs("tenant", "app", "DOC", List.of("foreign")))
                .thenReturn(List.of());
        assertThatThrownBy(() -> service.replaceFieldPolicies("role", "permission", "DOC",
                List.of(fieldRule("foreign", "ALLOW", "ALLOW"))))
                .isInstanceOf(SecurityException.class);
        verify(policyDao, never()).retireFieldPolicies(
                anyString(), anyString(), anyString(), anyString());
        verify(versionDao, never()).incrementForTenant(anyString());
    }

    @Test
    void shouldWriteOnlyRegisteredDelegableFieldsAndRejectDuplicates() {
        when(policyDao.selectDelegableFieldRefs("tenant", "app", "DOC", List.of("field-a")))
                .thenReturn(List.of("field-a"));
        when(policyDao.retireFieldPolicies("tenant", "app", "role", "permission")).thenReturn(0);
        when(policyDao.insertFieldPolicy(anyString(), eq("tenant"), eq("app"),
                eq("role"), eq("permission"), eq("field-a"),
                eq("MASK"), eq("DENY"), eq("admin"))).thenReturn(1);
        allowVersionAndAudit("FIELD_POLICY_REPLACE");

        service.replaceFieldPolicies("role", "permission", "DOC",
                List.of(fieldRule("field-a", "MASK", "DENY")));
        verify(versionDao).incrementForTenant("tenant");
        assertThatThrownBy(() -> service.replaceFieldPolicies("role", "permission", "DOC",
                List.of(fieldRule("field-a", "ALLOW", "ALLOW"),
                        fieldRule("field-a", "DENY", "DENY"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNonDelegablePermissionsAndPlatformRoles() {
        when(roleDao.selectDelegablePermissions("tenant", "app", List.of("permission")))
                .thenReturn(List.of());
        assertThatThrownBy(() -> service.replaceDataPolicies(
                "role", "permission", List.of(dataRule("SELF", null))))
                .isInstanceOf(SecurityException.class);
        RoleDO systemRole = new RoleDO();
        systemRole.setRoleType("SYSTEM");
        systemRole.setVersion(3L);
        when(roleDao.lockScopedRole("tenant", "app", "role")).thenReturn(systemRole);
        assertThatThrownBy(() -> service.replaceDataPolicies(
                "role", "permission", List.of(dataRule("SELF", null))))
                .isInstanceOf(SecurityException.class);
        verify(policyDao, never()).retireDataPolicies(
                anyString(), anyString(), anyString(), anyString());
    }

    private void allowVersionAndAudit(String operation) {
        when(roleDao.incrementRoleVersion("tenant", "app", "role", 5L, "admin"))
                .thenReturn(1);
        when(versionDao.incrementForTenant("tenant")).thenReturn(1);
        when(auditDao.append(anyString(), eq("tenant"), eq("app"),
                eq("admin"), eq("role"), eq(operation), eq(5L), eq(6L)))
                .thenReturn(1);
    }

    private static RolePermissionGrantVO permission(String owner) {
        RolePermissionGrantVO result = new RolePermissionGrantVO();
        result.setPermissionRefId("permission");
        result.setPermissionCode("DOC.READ");
        result.setOwnerType(owner);
        return result;
    }

    private static SaasDataPolicyUpdateDTO.Rule dataRule(String scope, String org) {
        SaasDataPolicyUpdateDTO.Rule result = new SaasDataPolicyUpdateDTO.Rule();
        result.setDataScope(scope);
        result.setOrgRefId(org);
        return result;
    }

    private static SaasFieldPolicyUpdateDTO.Rule fieldRule(String id, String read, String write) {
        SaasFieldPolicyUpdateDTO.Rule result = new SaasFieldPolicyUpdateDTO.Rule();
        result.setFieldRefId(id);
        result.setReadEffect(read);
        result.setWriteEffect(write);
        return result;
    }
}
