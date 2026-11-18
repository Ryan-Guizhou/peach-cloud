package com.peach.auth.openfeign.fallback;

import com.peach.auth.dto.SaasIamPermissionCheckDTO;
import com.peach.auth.openfeign.SaasIamFeignClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Feign 远端错误不可提升为任意用户的授权或身份。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 11:35
 */
class SaasIamFeignClientFallbackFactoryTest {

    @Test
    void shouldDenyActionAndRejectTrustedContextOnAnyRemoteFailure() {
        SaasIamFeignClient fallback = new SaasIamFeignClientFallbackFactory()
                .create(new IllegalStateException("unavailable"));
        assertThat(fallback.checkApiPermission("token", new SaasIamPermissionCheckDTO())
                .isAllowed()).isFalse();
        assertThatThrownBy(() -> fallback.requireContext("token"))
                .isInstanceOf(SecurityException.class);
    }
}
