package com.peach.auth.service.data;

import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.vo.RoleDataGrantVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 对租户、双层角色组织范围及跨绑定权限交叉越权进行回归测试。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 19:20
 */
class DataScopeEvaluatorTest {

    private final TenantSessionContext session =
            new TenantSessionContext("user-a", "tenant-a", "member-a", "app-a", 2L, 5L);

    private final OrganizationScopeMatcher hierarchy = (tenant, root, candidate) ->
            "tenant-a".equals(tenant) && (
                    root.equals(candidate) ||
                    ("org-root".equals(root) && ("org-left".equals(candidate) || "org-right".equals(candidate))));

    @Test
    void shouldKeepTenantAppAndLogicalDeleteIndependent() {
        RoleDataGrantVO grant = grant("b", "r", "p", "TENANT", null, "TENANT_ALL", null);
        assertThat(DataScopeEvaluator.matching(session,
                object("tenant-b", "app-a", "org-left", false), List.of(grant), hierarchy)).isEmpty();
        assertThat(DataScopeEvaluator.matching(session,
                object("tenant-a", "app-b", "org-left", false), List.of(grant), hierarchy)).isEmpty();
        assertThat(DataScopeEvaluator.matching(session,
                object("tenant-a", "app-a", "org-left", true), List.of(grant), hierarchy)).isEmpty();
    }

    @Test
    void shouldGrantTwoDistantOrganizationsButNotAThird() {
        RoleDataGrantVO left = grant("binding-left", "role-a", "permission-a",
                "ORG", "org-left", "TENANT_ALL", null);
        RoleDataGrantVO right = grant("binding-right", "role-a", "permission-a",
                "ORG", "org-right", "TENANT_ALL", null);

        assertThat(DataScopeEvaluator.matching(session, object("tenant-a", "app-a", "org-left", false),
                List.of(left, right), hierarchy)).containsExactly(left);
        assertThat(DataScopeEvaluator.matching(session, object("tenant-a", "app-a", "org-right", false),
                List.of(left, right), hierarchy)).containsExactly(right);
        assertThat(DataScopeEvaluator.matching(session, object("tenant-a", "app-a", "org-third", false),
                List.of(left, right), hierarchy)).isEmpty();
    }

    @Test
    void shouldIntersectSameRoleBindingWithItsOwnDataScope() {
        RoleDataGrantVO broadBindingNarrowPolicy = grant("binding-a", "role-a", "permission-a",
                "ORG_TREE", "org-root", "ORG", "org-left");
        RoleDataGrantVO narrowBindingBroadPolicy = grant("binding-b", "role-b", "permission-b",
                "ORG", "org-right", "TENANT_ALL", null);
        assertThat(DataScopeEvaluator.matching(session,
                object("tenant-a", "app-a", "org-third", false),
                List.of(broadBindingNarrowPolicy, narrowBindingBroadPolicy), hierarchy)).isEmpty();
        assertThat(DataScopeEvaluator.matching(session,
                object("tenant-a", "app-a", "org-left", false),
                List.of(broadBindingNarrowPolicy, narrowBindingBroadPolicy), hierarchy))
                .containsExactly(broadBindingNarrowPolicy);
    }

    @Test
    void shouldRequireMatchingOwnerForSelfScope() {
        RoleDataGrantVO grant = grant("binding", "role", "permission",
                "TENANT", null, "SELF", null);
        DataObjectDescriptor own = new DataObjectDescriptor(
                "tenant-a", "app-a", "object", "user-a", null, false);
        DataObjectDescriptor other = new DataObjectDescriptor(
                "tenant-a", "app-a", "object", "other-user", null, false);
        assertThat(DataScopeEvaluator.matching(session, own, List.of(grant), hierarchy))
                .containsExactly(grant);
        assertThat(DataScopeEvaluator.matching(session, other, List.of(grant), hierarchy)).isEmpty();
    }

    @Test
    void shouldFailClosedWhenOrganizationTreeEvidenceIsUnavailable() {
        RoleDataGrantVO grant = grant("binding", "role", "permission",
                "ORG_TREE", "org-root", "TENANT_ALL", null);
        assertThat(DataScopeEvaluator.matching(session,
                object("tenant-a", "app-a", "org-left", false),
                List.of(grant), null)).isEmpty();
    }

    @Test
    void shouldRejectDisabledDirectOrganizationAnchor() {
        RoleDataGrantVO grant = grant("b", "r", "p", "ORG", "disabled",
                "TENANT_ALL", null);
        OrganizationScopeMatcher directory = (tenant, root, candidate) -> false;
        assertThat(DataScopeEvaluator.matching(session,
                object("tenant-a", "app-a", "disabled", false),
                List.of(grant), directory)).isEmpty();
    }

    private static DataObjectDescriptor object(String tenant, String app,
                                               String org, boolean deleted) {
        return new DataObjectDescriptor(tenant, app, "object-id", "other-user", org, deleted);
    }

    private static RoleDataGrantVO grant(String bindingId, String roleId, String permissionId,
                                         String scope, String orgRefId,
                                         String dataScope, String dataOrgRefId) {
        RoleDataGrantVO grant = new RoleDataGrantVO();
        grant.setBindingRefId(bindingId);
        grant.setRoleRefId(roleId);
        grant.setPermissionRefId(permissionId);
        grant.setBindingScopeType(scope);
        grant.setBindingOrgRefId(orgRefId);
        grant.setDataScope(dataScope);
        grant.setDataOrgRefId(dataOrgRefId);
        return grant;
    }
}
