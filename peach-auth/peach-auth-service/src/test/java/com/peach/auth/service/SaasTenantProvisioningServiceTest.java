package com.peach.auth.service;

import com.peach.auth.dao.IamAuthorizationAuditDao;
import com.peach.auth.dao.MemberAuthVersionDao;
import com.peach.auth.dao.ScopedRoleBindingDao;
import com.peach.auth.dao.ScopedRoleManagementDao;
import com.peach.auth.dao.TenantMemberDao;
import com.peach.auth.dao.TenantProvisioningDao;
import com.peach.auth.dto.SaasTenantCreateDTO;
import com.peach.auth.entity.MemberRoleBindingDO;
import com.peach.auth.entity.RoleDO;
import com.peach.auth.entity.TenantMemberDO;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.auth.service.support.AuthBusinessIdGenerator;
import com.peach.auth.service.support.AuthBusinessIdType;
import com.peach.auth.vo.RolePermissionGrantVO;
import com.peach.auth.vo.SaasTenantCreatedVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 平台开通必须验证真实 SYSTEM 权限，并确保首次所有者原子初始化。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 19:15
 */
@ExtendWith(MockitoExtension.class)
class SaasTenantProvisioningServiceTest {

    private static final String OWNER_USER_REF_ID = "0123456789abcdef0123456789abcdef";

    @Mock
    private TenantSessionService sessionService;
    @Mock
    private TenantProvisioningDao provisioningDao;
    @Mock
    private TenantMemberDao memberDao;
    @Mock
    private ScopedRoleManagementDao roleDao;
    @Mock
    private ScopedRoleBindingDao bindingDao;
    @Mock
    private MemberAuthVersionDao versionDao;
    @Mock
    private IamAuthorizationAuditDao auditDao;
    @Mock
    private AuthBusinessIdGenerator generator;
    @InjectMocks
    private SaasTenantProvisioningService service;

    @Test
    void shouldRejectTenantCreationWithoutTrustedPlatformControlTenant() {
        when(sessionService.requireCurrent()).thenReturn(platformContext());
        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("platform");
        verifyNoInteractions(generator, roleDao, bindingDao);
    }

    @Test
    void shouldRejectStaleOrMissingProtectedPlatformPrivilege() {
        when(sessionService.requireCurrent()).thenReturn(platformContext());
        when(provisioningDao.lockPlatformTenant("platform")).thenReturn("platform");
        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(SecurityException.class);
        verify(provisioningDao).countPlatformCreateGrant("platform", "member", "app", 2L, 7L);
        verifyNoInteractions(generator, roleDao, bindingDao);
    }

    @Test
    void shouldRejectUninitializedTenantAuthorizationCatalogBeforeDatabaseWrites() {
        authorizeOperator();
        when(memberDao.countEnabledUser(OWNER_USER_REF_ID)).thenReturn(1);
        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("catalog");
        verifyNoInteractions(generator, roleDao, bindingDao);
    }

    @Test
    void shouldCreateTenantWithProtectedOwnerAndVersionedAudit() {
        authorizeOperator();
        when(memberDao.countEnabledUser(OWNER_USER_REF_ID)).thenReturn(1);
        List<RolePermissionGrantVO> permissions = ownerPermissions();
        when(provisioningDao.selectOwnerPermissions()).thenReturn(permissions);
        when(generator.next(any(AuthBusinessIdType.class)))
                .thenAnswer(call -> call.<AuthBusinessIdType>getArgument(0).name() + "_00000001");
        when(provisioningDao.insertTenant(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString())).thenReturn(1);
        when(provisioningDao.insertApplication(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString())).thenReturn(1);
        when(provisioningDao.insertRootOrganization(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString())).thenReturn(1);
        when(memberDao.insert(any(TenantMemberDO.class))).thenReturn(1);
        when(provisioningDao.insertOwnerRole(any(RoleDO.class))).thenReturn(1);
        when(provisioningDao.insertOwnerManagementDataPolicy(
                anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString())).thenReturn(1);
        when(roleDao.insertRolePermission(anyString(), anyString(), anyString(),
                anyString(), anyString())).thenReturn(1);
        when(bindingDao.insert(any(MemberRoleBindingDO.class))).thenReturn(1);
        when(versionDao.incrementForMember(anyString(), anyString())).thenReturn(1);
        when(auditDao.append(anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyLong(), anyLong())).thenReturn(1);

        SaasTenantCreatedVO created = service.create(request());

        assertThat(created.getTenantRefId()).hasSize(32);
        assertThat(created.getTenantId()).isEqualTo("TENANT_00000001");
        assertThat(created.getAppId()).isEqualTo("APP_00000001");
        assertThat(created.getOrganizationId()).isEqualTo("ORG_00000001");
        ArgumentCaptor<RoleDO> role = ArgumentCaptor.forClass(RoleDO.class);
        verify(provisioningDao).insertOwnerRole(role.capture());
        assertThat(role.getValue().getRoleCode()).isEqualTo("ROLE_TENANT_OWNER");
        assertThat(role.getValue().getRoleType()).isEqualTo("SYSTEM");
        verify(provisioningDao, times(3)).insertOwnerManagementDataPolicy(
                anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString());
        verify(roleDao, times(permissions.size())).insertRolePermission(
                anyString(), anyString(), anyString(), anyString(), anyString());
        verify(versionDao).incrementForMember(created.getTenantRefId(), created.getOwnerMemberRefId());
        verify(auditDao).append(anyString(), anyString(), anyString(), anyString(),
                anyString(), org.mockito.ArgumentMatchers.eq("TENANT_OWNER_GRANT"),
                org.mockito.ArgumentMatchers.eq(0L), org.mockito.ArgumentMatchers.eq(1L));
    }

    private void authorizeOperator() {
        when(sessionService.requireCurrent()).thenReturn(platformContext());
        when(provisioningDao.lockPlatformTenant("platform")).thenReturn("platform");
        when(provisioningDao.countPlatformCreateGrant("platform", "member", "app", 2L, 7L))
                .thenReturn(1);
    }

    private static TenantSessionContext platformContext() {
        return new TenantSessionContext("user", "platform", "member", "app", 2L, 7L);
    }

    private static SaasTenantCreateDTO request() {
        SaasTenantCreateDTO dto = new SaasTenantCreateDTO();
        dto.setTenantCode("ACME_TEAM");
        dto.setTenantName("Acme Team");
        dto.setAppName("Workspace");
        dto.setOwnerUserRefId(OWNER_USER_REF_ID);
        return dto;
    }

    private static List<RolePermissionGrantVO> ownerPermissions() {
        String[] codes = {
                "TENANT.MEMBER.MANAGE", "TENANT.ORG.MANAGE", "TENANT.ROLE.MANAGE",
                "POST:/api/auth/saas/tenants/members/invitations",
                "DELETE:/api/auth/saas/tenants/members/invitations/{invitationRefId}",
                "POST:/api/auth/saas/tenants/members/{memberRefId}/disable",
                "POST:/api/auth/saas/tenants/members/{memberRefId}/enable",
                "POST:/api/auth/saas/tenants/organizations",
                "PUT:/api/auth/saas/tenants/organizations/{orgRefId}",
                "DELETE:/api/auth/saas/tenants/organizations/{orgRefId}",
                "POST:/api/auth/saas/tenants/organizations/{orgRefId}/move"
        };
        List<RolePermissionGrantVO> values = new ArrayList<>();
        for (int i = 0; i < codes.length; i++) {
            RolePermissionGrantVO permission = new RolePermissionGrantVO();
            permission.setPermissionRefId("permission-" + i);
            permission.setPermissionCode(codes[i]);
            values.add(permission);
        }
        return values;
    }
}
