package com.peach.auth.service.data;

import com.peach.auth.dao.OrganizationHierarchyDao;
import com.peach.auth.entity.OrganizationDO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 机构树范围由同租户实际有效机构链判定，不信任客户端声明。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 19:20
 */
class DatabaseOrganizationScopeMatcherTest {

    @Test
    void shouldIncludeActiveDescendantAndItsAncestorItself() {
        OrganizationHierarchyDao dao = mock(OrganizationHierarchyDao.class);
        when(dao.findEnabledTenantBusinessId("tenant")).thenReturn("TENANT_BIZ");
        when(dao.selectInTenant("child", "TENANT_BIZ"))
                .thenReturn(org("child", "root", true));
        when(dao.selectInTenant("root", "TENANT_BIZ"))
                .thenReturn(org("root", null, true));
        DatabaseOrganizationScopeMatcher matcher = new DatabaseOrganizationScopeMatcher(dao);

        assertThat(matcher.contains("tenant", "root", "child")).isTrue();
        assertThat(matcher.contains("tenant", "root", "root")).isTrue();
        assertThat(matcher.contains("tenant", "unknown", "child")).isFalse();
    }

    @Test
    void shouldDenyDisabledAndCrossTenantOrganizations() {
        OrganizationHierarchyDao dao = mock(OrganizationHierarchyDao.class);
        when(dao.findEnabledTenantBusinessId("tenant")).thenReturn("TENANT_BIZ");
        when(dao.selectInTenant("disabled", "TENANT_BIZ"))
                .thenReturn(org("disabled", "root", false));
        DatabaseOrganizationScopeMatcher matcher = new DatabaseOrganizationScopeMatcher(dao);

        assertThat(matcher.contains("tenant", "root", "disabled")).isFalse();
        assertThat(matcher.contains("tenant", "root", "foreign")).isFalse();
        assertThat(matcher.contains("foreign-tenant", "root", "root")).isFalse();
    }

    @Test
    void shouldRejectCyclesInsteadOfLooping() {
        OrganizationHierarchyDao dao = mock(OrganizationHierarchyDao.class);
        when(dao.findEnabledTenantBusinessId("tenant")).thenReturn("TENANT_BIZ");
        when(dao.selectInTenant("child", "TENANT_BIZ"))
                .thenReturn(org("child", "parent", true));
        when(dao.selectInTenant("parent", "TENANT_BIZ"))
                .thenReturn(org("parent", "child", true));
        DatabaseOrganizationScopeMatcher matcher = new DatabaseOrganizationScopeMatcher(dao);

        assertThat(matcher.contains("tenant", "other", "child")).isFalse();
    }

    private static OrganizationDO org(String id, String parentRefId, boolean enabled) {
        OrganizationDO result = new OrganizationDO();
        result.setId(id);
        result.setParentOrgRefId(parentRefId);
        result.setStatus(enabled ? "ENABLE" : "DISABLED");
        result.setIsDelete(0);
        return result;
    }
}
