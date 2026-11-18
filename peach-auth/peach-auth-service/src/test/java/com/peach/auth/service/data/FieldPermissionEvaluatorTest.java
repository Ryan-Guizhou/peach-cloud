package com.peach.auth.service.data;

import com.peach.auth.vo.RoleDataGrantVO;
import com.peach.auth.vo.ScopedFieldRuleVO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 字段访问必须来自已通过行级验证的同绑定、同角色、同动作。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 19:20
 */
class FieldPermissionEvaluatorTest {

    @Test
    void shouldRemoveUnknownAndDeniedFieldsAndMaskSensitiveValues() {
        RoleDataGrantVO grant = grant("b1", "r1", "permission-read");
        List<ScopedFieldRuleVO> rules = List.of(
                rule("b1", "r1", "permission-read", "displayName", "ALLOW", "DENY", "FULL"),
                rule("b1", "r1", "permission-read", "mobile", "MASK", "DENY", "PHONE"),
                rule("b1", "r1", "permission-read", "password", "DENY", "DENY", "FULL"));
        Map<String, Object> output = FieldPermissionEvaluator.projectRead(
                Map.of("displayName", "Alice", "mobile", "13800001234",
                       "password", "secret", "unregistered", "private-value"),
                List.of(grant), rules);
        assertThat(output).containsEntry("displayName", "Alice")
                .containsEntry("mobile", "138****1234")
                .doesNotContainKeys("password", "unregistered");
    }

    @Test
    void shouldNeverMixFieldRulesFromDifferentPermissionsOrRoles() {
        RoleDataGrantVO read = grant("b1", "r1", "read-permission");
        List<ScopedFieldRuleVO> rules = List.of(
                rule("b1", "r2", "read-permission", "secret", "ALLOW", "ALLOW", "FULL"),
                rule("b1", "r1", "write-permission", "secret", "ALLOW", "ALLOW", "FULL"));
        assertThat(FieldPermissionEvaluator.projectRead(
                Map.of("secret", "value"), List.of(read), rules)).isEmpty();
        assertThatThrownBy(() -> FieldPermissionEvaluator.requireWrite(
                Map.of("secret", "changed"), List.of(read), rules))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void shouldPreferExplicitDenyAndMaskOverAllow() {
        RoleDataGrantVO a = grant("a", "role-a", "permission");
        RoleDataGrantVO b = grant("b", "role-b", "permission");
        List<ScopedFieldRuleVO> rules = List.of(
                rule("a", "role-a", "permission", "email", "ALLOW", "ALLOW", "EMAIL"),
                rule("b", "role-b", "permission", "email", "MASK", "DENY", "EMAIL"));
        assertThat(FieldPermissionEvaluator.projectRead(
                Map.of("email", "user@example.com"), List.of(a, b), rules))
                .containsEntry("email", "u***@example.com");
        assertThatThrownBy(() -> FieldPermissionEvaluator.requireWrite(
                Map.of("email", "new@example.com"), List.of(a, b), rules))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void shouldRejectPartialPatchInsteadOfSilentlyDiscardingFields() {
        RoleDataGrantVO grant = grant("binding", "role", "edit");
        List<ScopedFieldRuleVO> rules = List.of(
                rule("binding", "role", "edit", "name", "ALLOW", "ALLOW", "FULL"));
        assertThatThrownBy(() -> FieldPermissionEvaluator.requireWrite(
                Map.of("name", "Updated", "isAdmin", true), List.of(grant), rules))
                .isInstanceOf(SecurityException.class);
        FieldPermissionEvaluator.requireWrite(Map.of("name", "Updated"), List.of(grant), rules);
    }

    private static RoleDataGrantVO grant(String bindingId, String roleId, String permissionId) {
        RoleDataGrantVO result = new RoleDataGrantVO();
        result.setBindingRefId(bindingId);
        result.setRoleRefId(roleId);
        result.setPermissionRefId(permissionId);
        return result;
    }

    private static ScopedFieldRuleVO rule(String bindingId, String roleId, String permissionId,
                                          String field, String read, String write, String mask) {
        ScopedFieldRuleVO rule = new ScopedFieldRuleVO();
        rule.setBindingRefId(bindingId);
        rule.setRoleRefId(roleId);
        rule.setPermissionRefId(permissionId);
        rule.setFieldCode(field);
        rule.setReadEffect(read);
        rule.setWriteEffect(write);
        rule.setMaskStrategy(mask);
        return rule;
    }
}
