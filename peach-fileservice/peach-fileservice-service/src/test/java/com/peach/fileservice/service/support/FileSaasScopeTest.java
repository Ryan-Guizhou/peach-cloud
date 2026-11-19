package com.peach.fileservice.service.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 文件物理归属不可降级成仅按旧 TENANT_ID/ORG_ID 判断。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/19
 */
class FileSaasScopeTest {

    @Test
    void shouldFailClosedOnMissingPhysicalTenantOrApplication() {
        assertThatThrownBy(() -> new FileSaasScope(null, "app", "user", "member", "TENANT_01"))
                .isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> new FileSaasScope("tenant", "", "user", "member", "TENANT_01"))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void shouldFailClosedOnMissingOwnerOrMappedBusinessTenant() {
        assertThatThrownBy(() -> new FileSaasScope("tenant", "app", "", "member", "TENANT_01"))
                .isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> new FileSaasScope("tenant", "app", "user", "member", null))
                .isInstanceOf(SecurityException.class);
    }
}
