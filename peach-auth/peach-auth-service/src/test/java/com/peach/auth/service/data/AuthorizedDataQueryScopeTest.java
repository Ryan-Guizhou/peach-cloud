package com.peach.auth.service.data;

import com.peach.auth.dao.ScopedDataAuthorizationDao;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.auth.vo.RoleDataGrantVO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SQL 预筛选条件不得将不同角色绑定的操作与机构/所有者范围拼接。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 10:20
 */
class AuthorizedDataQueryScopeTest {

    private final TenantSessionContext context =
            new TenantSessionContext("user", "tenant", "member", "app", 1L, 2L);

    @Test
    void shouldIntersectIndependentGrantsAndCarryTenant() {
        OrganizationScopeDirectory directory = mock(OrganizationScopeDirectory.class);
        when(directory.descendants("tenant", "root")).thenReturn(Set.of("root", "left", "right"));
        when(directory.contains("tenant", "left", "left")).thenReturn(true);
        when(directory.contains("tenant", "other", "other")).thenReturn(true);
        RoleDataGrantVO first = grant("binding-left", "role-a", "p",
                "ORG_TREE", "root", "ORG", "left");
        RoleDataGrantVO second = grant("binding-other", "role-b", "p",
                "ORG", "other", "SELF", null);
        AuthorizedDataQueryScope scope = AuthorizedDataQueryScope.from(context,
                List.of(first, second), directory);

        assertThat(scope.tenantRefId()).isEqualTo("tenant");
        assertThat(scope.appRefId()).isEqualTo("app");
        assertThat(scope.clauses()).hasSize(2);
        assertThat(scope.clauses().get(0).allowedOrgRefIds()).containsExactly("left");
        assertThat(scope.clauses().get(0).requiredOwnerUserRefId()).isNull();
        assertThat(scope.clauses().get(1).allowedOrgRefIds()).containsExactly("other");
        assertThat(scope.clauses().get(1).requiredOwnerUserRefId()).isEqualTo("user");
        assertThat(scope.clauses().get(1).bindingRefId()).isEqualTo("binding-other");
        assertThatThrownBy(() -> scope.clauses().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldRefuseInvalidAnchorAndNonIntersectingScopes() {
        OrganizationScopeDirectory directory = mock(OrganizationScopeDirectory.class);
        when(directory.contains("tenant", "left", "left")).thenReturn(true);
        when(directory.contains("tenant", "right", "right")).thenReturn(true);
        assertThatThrownBy(() -> AuthorizedDataQueryScope.from(context,
                List.of(grant("b", "r", "p", "ORG", "left", "ORG", "right")), directory))
                .isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> AuthorizedDataQueryScope.from(context,
                List.of(grant("b", "r", "p", "TENANT", "forged", "TENANT_ALL", null)), directory))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void shouldRejectUnregisteredOrDisabledOrganizationRoots() {
        OrganizationScopeDirectory directory = mock(OrganizationScopeDirectory.class);
        assertThatThrownBy(() -> AuthorizedDataQueryScope.from(context,
                List.of(grant("b", "r", "p", "ORG", "disabled", "TENANT_ALL", null)), directory))
                .isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> AuthorizedDataQueryScope.from(context,
                List.of(grant("b", "r", "p", "INVALID", null, "TENANT_ALL", null)), directory))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void serviceMustTakeCurrentPrincipalAndDenyEmptyGrants() {
        TenantSessionService session = mock(TenantSessionService.class);
        ScopedDataAuthorizationDao dao = mock(ScopedDataAuthorizationDao.class);
        OrganizationScopeDirectory directory = mock(OrganizationScopeDirectory.class);
        when(session.requireCurrent()).thenReturn(context);
        when(dao.selectDataGrants("tenant", "member", "app", "DOC.READ"))
                .thenReturn(List.of(grant("b", "r", "p", "TENANT", null, "TENANT_ALL", null)));
        ScopedDataQueryScopeService service = new ScopedDataQueryScopeService(session, dao, directory);
        assertThat(service.requireScope("DOC.READ").clauses()).hasSize(1);
        assertThatThrownBy(() -> service.requireScope("DOC.WRITE")).isInstanceOf(SecurityException.class);
    }

    private static RoleDataGrantVO grant(String binding, String role, String permission,
                                         String bindingScope, String bindingOrg, String policyScope,
                                         String policyOrg) {
        RoleDataGrantVO value = new RoleDataGrantVO();
        value.setBindingRefId(binding);
        value.setRoleRefId(role);
        value.setPermissionRefId(permission);
        value.setBindingScopeType(bindingScope);
        value.setBindingOrgRefId(bindingOrg);
        value.setDataScope(policyScope);
        value.setDataOrgRefId(policyOrg);
        return value;
    }
}
