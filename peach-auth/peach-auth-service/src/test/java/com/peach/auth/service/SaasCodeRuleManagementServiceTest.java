package com.peach.auth.service;

import com.peach.auth.dao.OrganizationHierarchyDao;
import com.peach.auth.dao.SaasCodeRuleDao;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.auth.service.support.IamPermissionCodes;
import com.peach.auth.vo.SaasCodeRuleVO;
import com.peach.code.CodeRuleMaintenance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * SaaS 编码规则必须先完成受信物理租户与系统权限判断，才能调用规则维护器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/19 10:40
 */
@ExtendWith(MockitoExtension.class)
class SaasCodeRuleManagementServiceTest {

    private static final String BUSINESS_TENANT_ID = "TENANT_00000001";

    @Mock
    private TenantSessionService sessionService;

    @Mock
    private ScopedPermissionService permissionService;

    @Mock
    private OrganizationHierarchyDao hierarchyDao;

    @Mock
    private SaasCodeRuleDao codeRuleDao;

    @Mock
    private CodeRuleMaintenance ruleMaintenance;

    @InjectMocks
    private SaasCodeRuleManagementService service;

    @Test
    void shouldRejectPlatformRuleAccessWithoutProtectedSystemGrant() {
        when(sessionService.requireCurrent()).thenReturn(platform());
        when(hierarchyDao.findEnabledTenantBusinessId("platform")).thenReturn("PLATFORM_BIZ");
        when(codeRuleDao.findPlatformControlTenant("platform")).thenReturn("platform");

        assertThatThrownBy(() -> service.list(1, 20))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("platform");
        verify(codeRuleDao).countProtectedPlatformManagerGrant("platform", "member", "app", 2L, 7L);
        verifyNoInteractions(ruleMaintenance, permissionService);
    }

    @Test
    void shouldRejectNormalTenantWithoutFullTenantManagementGrant() {
        when(sessionService.requireCurrent()).thenReturn(tenant());
        when(hierarchyDao.findEnabledTenantBusinessId("tenant")).thenReturn(BUSINESS_TENANT_ID);

        assertThatThrownBy(() -> service.list(1, 20))
                .isInstanceOf(SecurityException.class);
        verify(permissionService).hasTenantPermission(
                "tenant", "member", "app", IamPermissionCodes.TENANT_CODE_RULE_MANAGE);
        verifyNoInteractions(ruleMaintenance);
    }

    @Test
    void shouldShowOnlyCurrentTenantRulesAndNeverAllocateForPreview() {
        permitTenant(false);
        when(codeRuleDao.selectRule(BUSINESS_TENANT_ID, "TENANT", "ORG"))
                .thenReturn(rule("ORG", "TENANT", "ENABLE", 9L, 2L));

        assertThat(service.preview("org")).isEqualTo("ORG_00000010");
        verify(codeRuleDao).selectRule(BUSINESS_TENANT_ID, "TENANT", "ORG");
        verifyNoInteractions(ruleMaintenance);
    }

    @Test
    void shouldRejectDisabledRulePreview() {
        permitTenant(false);
        when(codeRuleDao.selectRule(BUSINESS_TENANT_ID, "TENANT", "ORG"))
                .thenReturn(rule("ORG", "TENANT", "DISABLE", 9L, 2L));
        assertThatThrownBy(() -> service.preview("ORG"))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(ruleMaintenance);
    }

    @Test
    void shouldLockTenantAndPassTrustedActorAndVersionToMaintenance() {
        permitTenant(true);
        when(codeRuleDao.selectRule(BUSINESS_TENANT_ID, "TENANT", "ORG"))
                .thenReturn(rule("ORG", "TENANT", "ENABLE", 9L, 2L));

        service.changeStatus("org", false, 2L);

        verify(hierarchyDao).lockTenantAndFindBusinessId("tenant");
        verify(ruleMaintenance).changeStatus(BUSINESS_TENANT_ID, "ORG", false, 2L, "member");
    }

    @Test
    void shouldRejectForeignOrMissingRuleBeforeAnyUpdate() {
        permitTenant(true);
        assertThatThrownBy(() -> service.advanceWatermark("USER", 200L, 3L))
                .isInstanceOf(SecurityException.class);
        verify(codeRuleDao).selectRule(BUSINESS_TENANT_ID, "TENANT", "USER");
        verifyNoInteractions(ruleMaintenance);
    }

    @Test
    void shouldAllowProtectedPlatformWatermarkChangeOnlyAfterSystemEvidence() {
        when(sessionService.requireCurrent()).thenReturn(platform());
        when(hierarchyDao.lockTenantAndFindBusinessId("platform")).thenReturn("PLATFORM_BIZ");
        when(codeRuleDao.findPlatformControlTenant("platform")).thenReturn("platform");
        when(codeRuleDao.countProtectedPlatformManagerGrant(
                "platform", "member", "app", 2L, 7L)).thenReturn(1);
        when(codeRuleDao.selectRule("PLATFORM", "PLATFORM", "TENANT"))
                .thenReturn(rule("TENANT", "PLATFORM", "ENABLE", 20L, 4L));

        service.advanceWatermark("TENANT", 25L, 4L);

        verify(ruleMaintenance).advanceWatermark("PLATFORM", "TENANT", 25L, 4L, "member");
        verifyNoInteractions(permissionService);
    }

    @Test
    void shouldRejectUnboundedPaginationBeforeReadingUserSession() {
        assertThatThrownBy(() -> service.list(1001, 20))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.list(1, 51))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(sessionService, ruleMaintenance);
    }

    @Test
    void shouldRejectMalformedPrefixEvenWithTenantAccess() {
        permitTenant(true);
        assertThatThrownBy(() -> service.changeFormat("ORG_BAD", "_", 8, 2L))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(ruleMaintenance);
    }

    private void permitTenant(boolean write) {
        when(sessionService.requireCurrent()).thenReturn(tenant());
        if (write) {
            when(hierarchyDao.lockTenantAndFindBusinessId("tenant")).thenReturn(BUSINESS_TENANT_ID);
        } else {
            when(hierarchyDao.findEnabledTenantBusinessId("tenant")).thenReturn(BUSINESS_TENANT_ID);
        }
        when(permissionService.hasTenantPermission(
                "tenant", "member", "app", IamPermissionCodes.TENANT_CODE_RULE_MANAGE)).thenReturn(true);
    }

    private static SaasCodeRuleVO rule(String code, String scope, String status,
                                       long current, long version) {
        SaasCodeRuleVO row = new SaasCodeRuleVO();
        row.setCodePrefix(code);
        row.setScopeType(scope);
        row.setCodeSeparator("_");
        row.setMaxCodeWidth(8);
        row.setCurrentValue(current);
        row.setRuleVersion(version);
        row.setStatus(status);
        return row;
    }

    private static TenantSessionContext tenant() {
        return new TenantSessionContext("user", "tenant", "member", "app", 2L, 7L);
    }

    private static TenantSessionContext platform() {
        return new TenantSessionContext("user", "platform", "member", "app", 2L, 7L);
    }
}
