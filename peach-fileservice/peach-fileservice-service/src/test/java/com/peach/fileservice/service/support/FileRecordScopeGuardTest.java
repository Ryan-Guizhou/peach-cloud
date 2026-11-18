package com.peach.fileservice.service.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 文件详情、下载、变更和上传会话必须拒绝任何不完整或越权的历史作用域。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 16:15
 */
class FileRecordScopeGuardTest {

    @Test
    void shouldAcceptOnlyExactTenantAndOrganization() {
        assertThatCode(() -> FileRecordScopeGuard.requireSameScope(
                "tenant-A", "org-A", "tenant-A", "org-A"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldDenyCrossTenantEvenWhenOrganizationBusinessCodeMatches() {
        assertThatThrownBy(() -> FileRecordScopeGuard.requireSameScope(
                "tenant-A", "org-A", "tenant-B", "org-A"))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void shouldDenyCrossOrganizationEvenWithinSameTenant() {
        assertThatThrownBy(() -> FileRecordScopeGuard.requireSameScope(
                "tenant-A", "org-A", "tenant-A", "org-B"))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void shouldDenyMissingOrBlankTrustedAndStoredScope() {
        assertThatThrownBy(() -> FileRecordScopeGuard.requireSameScope(
                "", "org-A", "tenant-A", "org-A"))
                .isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> FileRecordScopeGuard.requireSameScope(
                "tenant-A", "org-A", null, "org-A"))
                .isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> FileRecordScopeGuard.requireSameScope(
                "tenant-A", "org-A", "tenant-A", ""))
                .isInstanceOf(SecurityException.class);
    }
}
