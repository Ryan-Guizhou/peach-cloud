package com.peach.auth.service;

import com.peach.auth.dao.SaasPolicyCatalogDao;
import com.peach.auth.dao.ScopedDataAuthorizationDao;
import com.peach.auth.dao.ScopedDataPolicyManagementDao;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.auth.vo.RoleDataGrantVO;
import com.peach.auth.vo.RoleDataPolicyVO;
import com.peach.auth.vo.RolePermissionGrantVO;
import com.peach.auth.vo.SaasCatalogEntryVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 角色动作策略读取必须经过当前令牌、完整委派证据与字段目录限定。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 16:00
 */
class SaasPolicyCatalogServiceTest {

    private static final String ROLE = "11111111111111111111111111111111";
    private static final String PERMISSION = "22222222222222222222222222222222";

    private final TenantSessionContext context =
            new TenantSessionContext("user", "tenant", "member", "app", 2026, 2, 7);
    private TenantSessionService session;
    private ScopedPermissionService permissionService;
    private ScopedDataAuthorizationDao authorizationDao;
    private ScopedDataPolicyManagementDao policyDao;
    private SaasPolicyCatalogDao dao;
    private SaasPolicyCatalogService service;

    @BeforeEach
    void setup() {
        session = mock(TenantSessionService.class);
        permissionService = mock(ScopedPermissionService.class);
        authorizationDao = mock(ScopedDataAuthorizationDao.class);
        policyDao = mock(ScopedDataPolicyManagementDao.class);
        dao = mock(SaasPolicyCatalogDao.class);
        service = new SaasPolicyCatalogService(session, permissionService, authorizationDao,
                policyDao, dao);
        when(session.requireCurrent()).thenReturn(context);
    }

    @Test
    void shouldDenyUnprivilegedMemberBeforeReadingFieldCatalog() {
        assertThatThrownBy(() -> service.fields("DOC", 1, 20)).isInstanceOf(SecurityException.class);
        verifyNoInteractions(dao);
    }

    @Test
    void shouldReturnOnlyBoundedDelegableFieldPages() {
        allowManage();
        SaasCatalogEntryVO a = new SaasCatalogEntryVO();
        a.setId("a");
        SaasCatalogEntryVO b = new SaasCatalogEntryVO();
        b.setId("b");
        when(dao.selectDelegableFields("tenant", "app", "DOC", 2, 0))
                .thenReturn(List.of(a, b));

        var result = service.fields("DOC", 1, 1);

        assertThat(result.getItems()).containsExactly(a);
        assertThat(result.isHasMore()).isTrue();
    }

    @Test
    void shouldRejectPermissionActionNotHeldByAdministrator() {
        allowManage();
        trustedAction("TENANT");
        assertThatThrownBy(() -> service.dataPolicies(ROLE, PERMISSION))
                .isInstanceOf(SecurityException.class);
        verifyNoInteractions(policyDao);
    }

    @Test
    void shouldRejectSystemActionWithoutCompleteTenantAllRowEvidence() {
        allowManage();
        trustedAction("SYSTEM");
        allowAction();
        assertThatThrownBy(() -> service.dataPolicies(ROLE, PERMISSION))
                .isInstanceOf(SecurityException.class);
        verifyNoInteractions(policyDao);
    }

    @Test
    void shouldReadSystemActionWithSingleCompleteDelegationEvidence() {
        allowManage();
        trustedAction("SYSTEM");
        allowAction();
        RoleDataGrantVO row = new RoleDataGrantVO();
        row.setBindingScopeType("TENANT");
        row.setDataScope("TENANT_ALL");
        when(authorizationDao.selectDataGrants("tenant", "member", "app", 2026, "DOC.VIEW"))
                .thenReturn(List.of(row));
        RoleDataPolicyVO policy = new RoleDataPolicyVO();
        policy.setDataScope("SELF");
        when(policyDao.selectDataPolicies("tenant", "app", ROLE, PERMISSION))
                .thenReturn(List.of(policy));

        assertThat(service.dataPolicies(ROLE, PERMISSION)).containsExactly(policy);
    }

    @Test
    void shouldRejectTruncatedDataPolicySnapshot() {
        allowManage();
        trustedAction("TENANT");
        allowAction();
        when(policyDao.selectDataPolicies("tenant", "app", ROLE, PERMISSION))
                .thenReturn(Collections.nCopies(33, new RoleDataPolicyVO()));

        assertThatThrownBy(() -> service.dataPolicies(ROLE, PERMISSION))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void shouldDenyUntrustedFieldResourceCode() {
        allowManage();
        trustedAction("TENANT");
        allowAction();

        assertThatThrownBy(() -> service.fieldPolicies(ROLE, PERMISSION, "DOC;DROP"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void allowManage() {
        when(permissionService.hasTenantPermission("tenant", "member", "app", 2026,
                "TENANT.ROLE.MANAGE")).thenReturn(true);
    }

    private void allowAction() {
        when(permissionService.hasTenantPermission("tenant", "member", "app", 2026,
                "DOC.VIEW")).thenReturn(true);
    }

    private void trustedAction(String owner) {
        RolePermissionGrantVO row = new RolePermissionGrantVO();
        row.setPermissionRefId(PERMISSION);
        row.setPermissionCode("DOC.VIEW");
        row.setOwnerType(owner);
        when(dao.selectScopedRoleAction("tenant", "app", 2026, ROLE, PERMISSION)).thenReturn(row);
    }
}
