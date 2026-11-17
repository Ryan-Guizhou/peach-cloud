package com.peach.auth.service.impl;

import com.peach.auth.dao.OrganizationHierarchyDao;
import com.peach.auth.entity.OrganizationDO;
import com.peach.auth.service.TenantMembershipPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 机构移动的租户边界、环路和幂等行为验证。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 17:10
 */
class OrganizationHierarchyServiceTest {

    private OrganizationHierarchyDao dao;
    private ObjectProvider<TenantMembershipPolicy> provider;
    private OrganizationHierarchyService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        dao = mock(OrganizationHierarchyDao.class);
        provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mock(TenantMembershipPolicy.class));
        when(dao.lockTenantAndFindBusinessId("tenant-ref")).thenReturn("TENANT_0001");
        service = new OrganizationHierarchyService(dao, provider);
    }

    @Test
    void shouldMoveWithinTenantUsingPhysicalParentId() {
        when(dao.selectInTenant("child", "TENANT_0001")).thenReturn(org("child", "old-parent", 3L));
        when(dao.selectInTenant("new-parent", "TENANT_0001")).thenReturn(org("new-parent", null, 1L));
        when(dao.updateParent("child", "TENANT_0001", "new-parent", 3L, "admin-member"))
                .thenReturn(1);
        service.move("tenant-ref", "admin-member", "child", "new-parent");
        verify(dao).updateParent("child", "TENANT_0001", "new-parent", 3L, "admin-member");
    }

    @Test
    void shouldRejectCrossTenantParent() {
        when(dao.selectInTenant("child", "TENANT_0001")).thenReturn(org("child", null, 0L));
        assertThatThrownBy(() -> service.move("tenant-ref", "admin-member", "child", "foreign"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("another tenant");
        verify(dao, never()).updateParent(anyString(), anyString(), any(), anyLong(), anyString());
    }

    @Test
    void shouldRejectCyclesEvenAcrossMultipleAncestors() {
        when(dao.selectInTenant("child", "TENANT_0001")).thenReturn(org("child", null, 0L));
        when(dao.selectInTenant("parent", "TENANT_0001")).thenReturn(org("parent", "child", 0L));
        assertThatThrownBy(() -> service.move("tenant-ref", "admin-member", "child", "parent"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cycle");
        verify(dao, never()).updateParent(anyString(), anyString(), any(), anyLong(), anyString());
    }

    @Test
    void shouldRejectMissingTrustedPolicy() {
        when(provider.getIfAvailable()).thenReturn(null);
        assertThatThrownBy(() -> service.move("tenant-ref", "admin-member", "child", null))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("unavailable");
        verify(dao, never()).updateParent(anyString(), anyString(), any(), anyLong(), anyString());
    }

    @Test
    void shouldAllowRootMoveAndCheckVersion() {
        when(dao.selectInTenant("child", "TENANT_0001")).thenReturn(org("child", "previous", 5L));
        when(dao.updateParent("child", "TENANT_0001", null, 5L, "admin-member")).thenReturn(1);
        service.move("tenant-ref", "admin-member", "child", null);
        verify(dao).updateParent("child", "TENANT_0001", null, 5L, "admin-member");
    }

    private static OrganizationDO org(String id, String parent, long version) {
        OrganizationDO org = new OrganizationDO();
        org.setId(id);
        org.setParentOrgRefId(parent);
        org.setVersion(version);
        org.setStatus("ENABLE");
        org.setIsDelete(0);
        return org;
    }
}
