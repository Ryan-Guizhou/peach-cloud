package com.peach.auth.service;

import com.peach.auth.dao.ScopedRoleGrantDao;
import com.peach.auth.vo.RoleScopeGrantVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 成员角色授权要求动作和租户级作用域来自同一条有效绑定。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 18:10
 */
class ScopedPermissionServiceTest {

    private ScopedRoleGrantDao dao;
    private ScopedPermissionService service;

    @BeforeEach
    void setUp() {
        dao = mock(ScopedRoleGrantDao.class);
        service = new ScopedPermissionService(dao);
    }

    @Test
    void shouldDenyWhenThereAreNoGrants() {
        assertThat(service.hasTenantPermission("tenant", "member", "app", 2026,
                "TENANT.MEMBER.MANAGE")).isFalse();
    }

    @Test
    void shouldAllowTenantActionOnlyWithATenantScopeGrant() {
        RoleScopeGrantVO grant = binding("TENANT", null);
        when(dao.selectGrants("tenant", "member", "app", 2026,
                "TENANT.MEMBER.MANAGE")).thenReturn(List.of(grant));
        assertThat(service.hasTenantPermission("tenant", "member", "app", 2026,
                "TENANT.MEMBER.MANAGE")).isTrue();
        verify(dao).selectGrants("tenant", "member", "app", 2026,
                "TENANT.MEMBER.MANAGE");
    }

    @Test
    void shouldNotPromoteOrganizationScopeToTenantAdministration() {
        when(dao.selectGrants("tenant", "member", "app", 2026,
                "TENANT.MEMBER.MANAGE"))
                .thenReturn(List.of(binding("ORG", "org-a"), binding("ORG_TREE", "org-b")));
        assertThat(service.hasTenantPermission("tenant", "member", "app", 2026,
                "TENANT.MEMBER.MANAGE")).isFalse();
    }

    @Test
    void shouldKeepNaturalYearsIndependent() {
        when(dao.selectGrants("tenant", "member", "app", 2026,
                "TENANT.MEMBER.MANAGE")).thenReturn(List.of(binding("TENANT", null)));
        assertThat(service.hasTenantPermission("tenant", "member", "app", 2026,
                "TENANT.MEMBER.MANAGE")).isTrue();
        assertThat(service.hasTenantPermission("tenant", "member", "app", 2027,
                "TENANT.MEMBER.MANAGE")).isFalse();
        verify(dao).selectGrants("tenant", "member", "app", 2027,
                "TENANT.MEMBER.MANAGE");
    }

    @Test
    void shouldRejectMissingIdentityAndInvalidYearBeforeDatabaseAccess() {
        assertThat(service.hasTenantPermission("tenant", "", "app", 2026, "READ")).isFalse();
        assertThat(service.hasTenantPermission("tenant", "member", "app", 0, "READ")).isFalse();
        verify(dao, never()).selectGrants(anyString(), anyString(), anyString(), anyInt(), anyString());
    }

    private static RoleScopeGrantVO binding(String scopeType, String scopeRefId) {
        RoleScopeGrantVO grant = new RoleScopeGrantVO();
        grant.setScopeType(scopeType);
        grant.setScopeRefId(scopeRefId);
        grant.setPermissionCode("TENANT.MEMBER.MANAGE");
        return grant;
    }
}
