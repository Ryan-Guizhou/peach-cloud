package com.peach.auth.service.support;

import com.peach.auth.vo.LoginTargetVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 全局账号登录目标选择安全回归。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 17:50
 */
class LoginTargetSelectorTest {

    @Test
    void shouldSelectTheOnlyEnabledTenantApplication() {
        LoginTargetVO available = option("tenant-a", "app-a");
        assertThat(LoginTargetSelector.select(List.of(available), null, null))
                .isSameAs(available);
    }

    @Test
    void shouldRequireExplicitChoiceForMultipleTenantApplications() {
        LoginTargetVO first = option("tenant-a", "app-a");
        LoginTargetVO second = option("tenant-b", "app-b");
        assertThat(LoginTargetSelector.select(List.of(first, second), null, null)).isNull();
        assertThat(LoginTargetSelector.select(List.of(first, second), "tenant-b", "app-b"))
                .isSameAs(second);
    }

    @Test
    void shouldRejectForeignTenantOrForgedApplication() {
        List<LoginTargetVO> candidates = List.of(option("tenant-a", "app-a"));
        assertThatThrownBy(() -> LoginTargetSelector.select(candidates, "tenant-b", "app-a"))
                .isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> LoginTargetSelector.select(candidates, "tenant-a", "app-b"))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void shouldRejectPartialSelectionAndNoActiveMembership() {
        List<LoginTargetVO> candidates = List.of(option("tenant-a", "app-a"));
        assertThatThrownBy(() -> LoginTargetSelector.select(candidates, "tenant-a", null))
                .isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> LoginTargetSelector.select(List.of(), null, null))
                .isInstanceOf(SecurityException.class);
    }

    private static LoginTargetVO option(String tenantRefId, String appRefId) {
        LoginTargetVO result = new LoginTargetVO();
        result.setTenantRefId(tenantRefId);
        result.setAppRefId(appRefId);
        result.setTenantId("TENANT_BIZ");
        result.setAppId("APP_BIZ");
        return result;
    }
}
