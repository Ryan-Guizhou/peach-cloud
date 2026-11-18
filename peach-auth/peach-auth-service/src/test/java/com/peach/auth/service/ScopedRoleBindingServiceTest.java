package com.peach.auth.service;

import com.peach.auth.dao.IamAuthorizationAuditDao;
import com.peach.auth.dao.MemberAuthVersionDao;
import com.peach.auth.dao.OrganizationHierarchyDao;
import com.peach.auth.dao.ScopedRoleBindingDao;
import com.peach.auth.entity.TenantMemberDO;
import com.peach.auth.entity.MemberRoleBindingDO;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.auth.service.support.IamPermissionCodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 成员授权变更必须单条隔离、拒绝不可委派权限并同步递增版本和审计。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 18:10
 */
class ScopedRoleBindingServiceTest {

    private TenantMembershipPolicy policy;
    private ScopedPermissionService permissionService;
    private ScopedRoleBindingDao bindingDao;
    private OrganizationHierarchyDao hierarchyDao;
    private MemberAuthVersionDao versionDao;
    private IamAuthorizationAuditDao auditDao;
    private ScopedRoleBindingService service;

    @BeforeEach
    void prepare() {
        policy = mock(TenantMembershipPolicy.class);
        TenantSessionService sessionService = mock(TenantSessionService.class);
        permissionService = mock(ScopedPermissionService.class);
        bindingDao = mock(ScopedRoleBindingDao.class);
        hierarchyDao = mock(OrganizationHierarchyDao.class);
        versionDao = mock(MemberAuthVersionDao.class);
        auditDao = mock(IamAuthorizationAuditDao.class);

        service = new ScopedRoleBindingService(policy, sessionService, permissionService,
                bindingDao, hierarchyDao, versionDao, auditDao);
        when(sessionService.requireCurrent()).thenReturn(
                new TenantSessionContext("user", "tenant", "admin", "app", 0L, 1L));
        when(hierarchyDao.lockTenantAndFindBusinessId("tenant")).thenReturn("TENANT_BIZ");
    }

    @Test
    void shouldGrantRoleAndIncrementAuthorizationVersionAfterAudit() {
        prepareDelegableRole();
        when(bindingDao.insert(any())).thenReturn(1);
        when(versionDao.incrementForMember("tenant", "target")).thenReturn(1);
        when(auditDao.append(anyString(), eq("tenant"), eq("app"),
                eq("admin"), anyString(), eq("ROLE_GRANT"), eq(7L), eq(8L)))
                .thenReturn(1);

        String bindingRefId = service.grant("tenant", "admin", "target", "role", "TENANT", null);
        assertThat(bindingRefId).isNotBlank();
        verify(policy, times(2)).requireTenantPermission("tenant", "admin", IamPermissionCodes.TENANT_ROLE_MANAGE);
        verify(bindingDao).insert(any());
        verify(versionDao).incrementForMember("tenant", "target");
        verify(auditDao).append(anyString(), eq("tenant"), eq("app"),
                eq("admin"), eq(bindingRefId), eq("ROLE_GRANT"), eq(7L), eq(8L));
    }

    @Test
    void shouldDenyNonDelegablePermissionsWithoutWriting() {
        prepareDelegableRole();
        when(bindingDao.selectDelegablePermissionCodes("tenant", "app", "role"))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.grant("tenant", "admin", "target", "role", "TENANT", null))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("non-delegable");
        verify(bindingDao, never()).insert(any());
        verify(versionDao, never()).incrementForMember(anyString(), anyString());
    }

    @Test
    void shouldRejectCrossTenantOrganizationScope() {
        assertThatThrownBy(() -> service.grant("tenant", "admin", "target", "role", "ORG", "foreign"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("another tenant");
        verify(bindingDao, never()).insert(any());
    }

    @Test
    void shouldRejectSelfGrant() {
        assertThatThrownBy(() -> service.grant("tenant", "admin", "admin", "role", "TENANT", null))
                .isInstanceOf(SecurityException.class);
        verify(bindingDao, never()).insert(any());
    }

    @Test
    void shouldFailWhenAuditInsertFails() {
        prepareDelegableRole();
        when(bindingDao.insert(any())).thenReturn(1);
        when(versionDao.incrementForMember("tenant", "target")).thenReturn(1);
        assertThatThrownBy(() -> service.grant("tenant", "admin", "target", "role", "TENANT", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("audit");
    }

    @Test
    void shouldNotGrantAcrossApplications() {
        when(bindingDao.lockActiveMember("tenant", "target")).thenReturn(member());
        assertThatThrownBy(() -> service.grant("tenant", "admin", "target", "role", "TENANT", null))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Role is not active");
        verify(bindingDao, never()).insert(any());
    }

    @Test
    void shouldRevokeOneBindingAndIncrementAuthorizationVersion() {
        MemberRoleBindingDO binding = new MemberRoleBindingDO();
        binding.setId("binding-id");
        binding.setAppRefId("app");
        binding.setMemberRefId("target");
        binding.setRoleRefId("role");
        binding.setVersion(2L);
        when(bindingDao.selectActiveById("tenant", "binding-id")).thenReturn(binding);
        prepareDelegableRole();
        when(bindingDao.revoke("tenant", "binding-id", 2L)).thenReturn(1);
        when(versionDao.incrementForMember("tenant", "target")).thenReturn(1);
        when(auditDao.append(anyString(), eq("tenant"), eq("app"),
                eq("admin"), eq("binding-id"), eq("ROLE_REVOKE"), eq(7L), eq(8L)))
                .thenReturn(1);

        service.revoke("tenant", "admin", "binding-id");

        verify(bindingDao).revoke("tenant", "binding-id", 2L);
        verify(versionDao).incrementForMember("tenant", "target");
        verify(auditDao).append(anyString(), eq("tenant"), eq("app"),
                eq("admin"), eq("binding-id"), eq("ROLE_REVOKE"), eq(7L), eq(8L));
    }

    @Test
    void shouldNeverRevokeProtectedOwnerBinding() {
        MemberRoleBindingDO binding = new MemberRoleBindingDO();
        binding.setId("binding-id");
        binding.setAppRefId("app");
        binding.setMemberRefId("target");
        binding.setVersion(2L);
        when(bindingDao.selectActiveById("tenant", "binding-id")).thenReturn(binding);
        when(bindingDao.countProtectedOwnerBinding("tenant", "binding-id")).thenReturn(1);

        assertThatThrownBy(() -> service.revoke("tenant", "admin", "binding-id"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("owner");
        verify(bindingDao, never()).revoke("tenant", "binding-id", 2L);
    }

    @Test
    void shouldDenyRevokingLastTenantAdministratorBinding() {
        MemberRoleBindingDO binding = new MemberRoleBindingDO();
        binding.setId("binding-id");
        binding.setAppRefId("app");
        binding.setMemberRefId("target");
        binding.setVersion(2L);
        when(bindingDao.selectActiveById("tenant", "binding-id")).thenReturn(binding);
        when(bindingDao.countAdministratorBinding("tenant", "binding-id")).thenReturn(1);
        when(bindingDao.countActiveTenantAdministratorBindings("tenant")).thenReturn(1);

        assertThatThrownBy(() -> service.revoke("tenant", "admin", "binding-id"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Last tenant administrator");
        verify(bindingDao, never()).revoke("tenant", "binding-id", 2L);
    }

    private void prepareDelegableRole() {
        when(bindingDao.lockActiveMember("tenant", "target")).thenReturn(member());
        when(bindingDao.countActiveRole("tenant", "app", "role")).thenReturn(1);
        when(bindingDao.countActiveRolePermissions("tenant", "role")).thenReturn(1);
        when(bindingDao.selectDelegablePermissionCodes("tenant", "app", "role"))
                .thenReturn(List.of("RESOURCE.VIEW"));
        when(permissionService.hasTenantPermission("tenant", "admin", "app", "RESOURCE.VIEW"))
                .thenReturn(true);
    }

    private static TenantMemberDO member() {
        TenantMemberDO member = new TenantMemberDO();
        member.setId("target");
        member.setTenantRefId("tenant");
        member.setStatus("ACTIVE");
        member.setIsDelete(0);
        member.setAuthVersion(7L);
        return member;
    }
}
