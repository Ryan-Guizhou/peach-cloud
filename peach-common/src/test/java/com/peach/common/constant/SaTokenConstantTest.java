package com.peach.common.constant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sa-Token 多租户授权缓存 Key 测试。
 */
class SaTokenConstantTest {

    @Test
    void shouldIsolateApiPermissionCacheByTenantOrganizationAndFiscal() {
        String first = SaTokenConstant.userApiResourceCacheKey("user-1", "tenant-a", "org-a", "2026");
        String second = SaTokenConstant.userApiResourceCacheKey("user-1", "tenant-b", "org-a", "2026");
        String third = SaTokenConstant.userApiResourceCacheKey("user-1", "tenant-a", "org-b", "2026");
        String fourth = SaTokenConstant.userApiResourceCacheKey("user-1", "tenant-a", "org-a", "2025");

        assertThat(first).isNotEqualTo(second);
        assertThat(first).isNotEqualTo(third);
        assertThat(first).isNotEqualTo(fourth);
        assertThat(SaTokenConstant.userApiResourceCacheKey("user-1", "tenant-a", "org-a", "2026"))
                .isEqualTo(first);
    }
}
