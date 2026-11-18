package com.peach.auth.service.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 成员 Token 上下文只接受完整的物理关联与有效版本。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 17:35
 */
class TenantSessionContextTest {

    @Test
    void shouldRetainIndependentPhysicalIdentifiers() {
        TenantSessionContext context = new TenantSessionContext(
                "user-ref", "tenant-ref", "member-ref", "app-ref", 2L, 3L);
        assertThat(context.userRefId()).isEqualTo("user-ref");
        assertThat(context.tenantRefId()).isEqualTo("tenant-ref");
        assertThat(context.memberRefId()).isEqualTo("member-ref");
        assertThat(context.appRefId()).isEqualTo("app-ref");
        assertThat(context.memberVersion()).isEqualTo(2L);
        assertThat(context.authVersion()).isEqualTo(3L);
    }

    @Test
    void shouldRejectMissingContextAndStaleInvalidVersion() {
        assertThatThrownBy(() -> new TenantSessionContext("u", "", "m", "a", 0, 0))
                .isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> new TenantSessionContext("u", "t", null, "a", 0, 0))
                .isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> new TenantSessionContext("u", "t", "m", "a", -1, 0))
                .isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> new TenantSessionContext("u", "t", "m", "a", 1L, -1L))
                .isInstanceOf(SecurityException.class);
    }
}
