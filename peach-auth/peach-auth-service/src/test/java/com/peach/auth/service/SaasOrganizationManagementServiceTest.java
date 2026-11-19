package com.peach.auth.service;

import com.peach.auth.dao.IamAuthorizationAuditDao;
import com.peach.auth.dao.MemberAuthVersionDao;
import com.peach.auth.dao.OrganizationHierarchyDao;
import com.peach.auth.dao.SaasOrganizationManagementDao;
import com.peach.auth.dto.SaasOrganizationCreateDTO;
import com.peach.auth.entity.OrganizationDO;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.auth.service.support.AuthBusinessIdGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * 机构创建、并发修改和删除必须在受信租户与依赖约束下执行。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 19:15
 */
@ExtendWith(MockitoExtension.class)
class SaasOrganizationManagementServiceTest {

    @Mock
    private TenantSessionService sessionService;
    @Mock
    private TenantMembershipPolicy membershipPolicy;
    @Mock
    private OrganizationHierarchyDao hierarchyDao;
    @Mock
    private SaasOrganizationManagementDao managementDao;
    @Mock
    private MemberAuthVersionDao versionDao;
    @Mock
    private IamAuthorizationAuditDao auditDao;
    @Mock
    private AuthBusinessIdGenerator generator;
    @InjectMocks
    private SaasOrganizationManagementService service;

    @Test
    void shouldRejectCreatingOrganizationUnderForeignParent() {
        authorizeCurrentTenant();
        SaasOrganizationCreateDTO dto = new SaasOrganizationCreateDTO();
        dto.setOrgCode("BRANCH");
        dto.setOrgName("Branch");
        dto.setParentOrgRefId("foreign");
        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("another tenant");
        verify(managementDao, never()).insert(any(OrganizationDO.class));
        verifyNoInteractions(generator, versionDao);
    }

    @Test
    void shouldProtectDefaultRootFromDeletion() {
        authorizeCurrentTenant();
        when(hierarchyDao.selectInTenant("root", "TENANT_0001"))
                .thenReturn(organization("DEFAULT", 0L));
        assertThatThrownBy(() -> service.retire("root"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("root");
        verify(managementDao, never()).retire(anyString(), anyString(), anyLong(), anyString());
        verifyNoInteractions(versionDao, auditDao);
    }

    @Test
    void shouldRejectOrganizationDeletionWithExistingReferences() {
        authorizeCurrentTenant();
        when(hierarchyDao.selectInTenant("branch", "TENANT_0001"))
                .thenReturn(organization("BRANCH", 1L));
        when(managementDao.countDependencies("tenant", "TENANT_0001", "branch", "ORG_0001"))
                .thenReturn(2);
        assertThatThrownBy(() -> service.retire("branch"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("dependent");
        verify(managementDao, never()).retire(anyString(), anyString(), anyLong(), anyString());
    }

    @Test
    void shouldRejectConcurrentOrganizationRenameBeforeInvalidatingAuthorization() {
        authorizeCurrentTenant();
        OrganizationDO org = organization("BRANCH", 3L);
        org.setOrgName("Previous");
        when(hierarchyDao.selectInTenant("branch", "TENANT_0001")).thenReturn(org);
        assertThatThrownBy(() -> service.rename("branch", "New Name"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Concurrent");
        verifyNoInteractions(versionDao, auditDao);
    }

    private void authorizeCurrentTenant() {
        when(sessionService.requireCurrent()).thenReturn(
                new TenantSessionContext("user", "tenant", "member", "app", 2L, 7L));
        when(hierarchyDao.lockTenantAndFindBusinessId("tenant")).thenReturn("TENANT_0001");
    }

    private static OrganizationDO organization(String code, long version) {
        OrganizationDO org = new OrganizationDO();
        org.setId("branch");
        org.setOrgId("ORG_0001");
        org.setOrgCode(code);
        org.setStatus("ENABLE");
        org.setIsDelete(0);
        org.setVersion(version);
        return org;
    }
}
