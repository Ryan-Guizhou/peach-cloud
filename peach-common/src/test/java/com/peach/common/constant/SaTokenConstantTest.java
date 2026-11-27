package com.peach.common.constant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sa-Token 多租户授权缓存 Key 测试。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/06
 */
class SaTokenConstantTest {

    @Test
    void shouldIsolateApiPermissionCacheByTenantAndOrganization() {
        String first = SaTokenConstant.userApiResourceCacheKey("user-1", "tenant-a", "org-a");
        String second = SaTokenConstant.userApiResourceCacheKey("user-1", "tenant-b", "org-a");
        String third = SaTokenConstant.userApiResourceCacheKey("user-1", "tenant-a", "org-b");

        assertThat(first).isNotEqualTo(second);
        assertThat(first).isNotEqualTo(third);
        assertThat(SaTokenConstant.userApiResourceCacheKey("user-1", "tenant-a", "org-a"))
                .isEqualTo(first);
    }
}
