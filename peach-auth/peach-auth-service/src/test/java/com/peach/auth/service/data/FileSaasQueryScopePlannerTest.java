package com.peach.auth.service.data;

import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.vo.RoleDataGrantVO;
import com.peach.auth.vo.SaasIamFileQueryScopeVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 同一角色绑定内先进行机构与数据策略交集，然后才合并不同绑定的行范围。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/19
 */
@ExtendWith(MockitoExtension.class)
class FileSaasQueryScopePlannerTest {

    private static final TenantSessionContext CONTEXT =
            new TenantSessionContext("user", "tenant", "member", "app", 2L, 7L);

    @Mock
    private OrganizationScopeDirectory directory;

    @Test
    void shouldExposeOnlyCurrentOwnerWhenTenantBindingHasSelfPolicy() {
        SaasIamFileQueryScopeVO result = FileSaasQueryScopePlanner.plan(
                CONTEXT, List.of(grant("TENANT", null, "SELF", null)), directory);
        assertThat(result.isSelf()).isTrue();
        assertThat(result.isTenantAll()).isFalse();
        assertThat(result.getTenantRefId()).isEqualTo("tenant");
        assertThat(result.getAppRefId()).isEqualTo("app");
        assertThat(result.getOrganizationRefIds()).isEmpty();
        verifyNoInteractions(directory);
    }

    @Test
    void shouldExposeTenantAllOnlyWhenSameBindingAndPolicyPermitIt() {
        SaasIamFileQueryScopeVO result = FileSaasQueryScopePlanner.plan(
                CONTEXT, List.of(grant("TENANT", null, "TENANT_ALL", null)), directory);
        assertThat(result.isTenantAll()).isTrue();
        verifyNoInteractions(directory);
    }

    @Test
    void shouldIntersectBindingAndDataPolicyOrganizationTrees() {
        when(directory.descendants("tenant", "parent"))
                .thenReturn(Set.of("parent", "child", "unrelated"));
        when(directory.descendants("tenant", "child"))
                .thenReturn(Set.of("child", "grandchild"));
        SaasIamFileQueryScopeVO result = FileSaasQueryScopePlanner.plan(
                CONTEXT, List.of(grant("ORG_TREE", "parent", "ORG_TREE", "child")), directory);
        assertThat(result.isTenantAll()).isFalse();
        assertThat(result.isSelf()).isFalse();
        assertThat(result.getOrganizationRefIds()).containsExactly("child");
    }

    @Test
    void shouldKeepOwnOrganizationRangeSeparateFromAllOwners() {
        when(directory.descendants("tenant", "root"))
                .thenReturn(Set.of("root", "branch"));
        SaasIamFileQueryScopeVO result = FileSaasQueryScopePlanner.plan(
                CONTEXT, List.of(grant("ORG_TREE", "root", "SELF", null)), directory);
        assertThat(result.isTenantAll()).isFalse();
        assertThat(result.isSelf()).isFalse();
        assertThat(result.getOrganizationRefIds()).isEmpty();
        assertThat(result.getSelfOrganizationRefIds()).containsExactly("branch", "root");
    }

    @Test
    void shouldUnionOnlyEachIndependentlyCompleteBindingScope() {
        when(directory.contains("tenant", "branch", "branch")).thenReturn(true);
        SaasIamFileQueryScopeVO result = FileSaasQueryScopePlanner.plan(
                CONTEXT, List.of(grant("ORG", "branch", "TENANT_ALL", null),
                        grant("TENANT", null, "SELF", null)), directory);
        assertThat(result.isSelf()).isTrue();
        assertThat(result.isTenantAll()).isFalse();
        assertThat(result.getOrganizationRefIds()).containsExactly("branch");
    }

    @Test
    void shouldRejectDisjointOrganizationIntersection() {
        when(directory.contains("tenant", "branch-a", "branch-a")).thenReturn(true);
        when(directory.contains("tenant", "branch-b", "branch-b")).thenReturn(true);
        assertThatThrownBy(() -> FileSaasQueryScopePlanner.plan(CONTEXT,
                List.of(grant("ORG", "branch-a", "ORG", "branch-b")), directory))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void shouldRejectMissingOrIncompleteOrganizationTree() {
        when(directory.descendants("tenant", "root")).thenReturn(Set.of("orphan"));
        assertThatThrownBy(() -> FileSaasQueryScopePlanner.plan(CONTEXT,
                List.of(grant("ORG_TREE", "root", "TENANT_ALL", null)), directory))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void shouldRejectIncorrectRoleBindingAnchor() {
        assertThatThrownBy(() -> FileSaasQueryScopePlanner.plan(CONTEXT,
                List.of(grant("TENANT", "foreign-org", "TENANT_ALL", null)), directory))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void shouldRejectEmptyGrantListInsteadOfDefaultingToTenantAll() {
        assertThatThrownBy(() -> FileSaasQueryScopePlanner.plan(
                CONTEXT, List.of(), directory)).isInstanceOf(SecurityException.class);
        verifyNoInteractions(directory);
    }

    private static RoleDataGrantVO grant(String binding, String bindingOrg,
                                         String policy, String policyOrg) {
        RoleDataGrantVO result = new RoleDataGrantVO();
        result.setBindingRefId("binding");
        result.setRoleRefId("role");
        result.setPermissionRefId("permission");
        result.setBindingScopeType(binding);
        result.setBindingOrgRefId(bindingOrg);
        result.setDataScope(policy);
        result.setDataOrgRefId(policyOrg);
        return result;
    }
}
