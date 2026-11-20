package com.peach.auth.service;

import com.peach.auth.dao.TenantProvisioningDao;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 全局用户目录只能由 PLATFORM 控制租户下的保护角色操作。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/20 10:20
 */
@ExtendWith(MockitoExtension.class)
class PlatformUserManagementAuthorizationServiceTest {

    @Mock
    private TenantSessionService tenantSessionService;
    @Mock
    private TenantProvisioningDao provisioningDao;
    @InjectMocks
    private PlatformUserManagementAuthorizationService service;

    @Test
    void shouldAllowVerifiedPlatformAdministrator() {
        when(tenantSessionService.requireCurrent()).thenReturn(platformContext());
        when(provisioningDao.lockPlatformTenant("platform")).thenReturn("platform");
        when(provisioningDao.countPlatformCreateGrant("platform", "member", "app", 2L, 7L))
                .thenReturn(1);

        assertThatCode(service::requirePlatformAdministrator).doesNotThrowAnyException();
        verify(provisioningDao).countPlatformCreateGrant("platform", "member", "app", 2L, 7L);
    }

    @Test
    void shouldRejectOrdinaryTenantBeforeGrantQuery() {
        when(tenantSessionService.requireCurrent()).thenReturn(
                new TenantSessionContext("user", "tenant-a", "member", "app", 2, 7));

        assertThatThrownBy(service::requirePlatformAdministrator)
                .isInstanceOf(SecurityException.class);
        verify(provisioningDao).lockPlatformTenant("tenant-a");
    }

    @Test
    void shouldRejectMissingOrRevokedPlatformPrivilege() {
        when(tenantSessionService.requireCurrent()).thenReturn(platformContext());
        when(provisioningDao.lockPlatformTenant("platform")).thenReturn("platform");

        assertThatThrownBy(service::requirePlatformAdministrator)
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("platform");
        verify(provisioningDao).countPlatformCreateGrant("platform", "member", "app", 2L, 7L);
    }

    @Test
    void shouldRejectExpiredSessionWithoutDatabaseAuthorizationQuery() {
        when(tenantSessionService.requireCurrent())
                .thenThrow(new SecurityException("Expired membership"));

        assertThatThrownBy(service::requirePlatformAdministrator)
                .isInstanceOf(SecurityException.class);
        verifyNoInteractions(provisioningDao);
    }

    private static TenantSessionContext platformContext() {
        return new TenantSessionContext("user", "platform", "member", "app", 2, 7);
    }
}
