package com.peach.fileservice.service.support;

import cn.dev33.satoken.stp.StpUtil;
import com.peach.auth.dto.SaasIamPermissionCheckDTO;
import com.peach.auth.openfeign.SaasIamFeignClient;
import com.peach.auth.vo.SaasIamContextVO;
import com.peach.auth.vo.SaasIamPermissionDecisionVO;
import com.peach.fileservice.dao.FileSaasScopeDao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 最终用户身份必须来自在线 Feign 权限事实，旧业务租户字段不能提升权限。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/19
 */
@ExtendWith(MockitoExtension.class)
class FileSaasAccessServiceTest {

    @Mock
    private SaasIamFeignClient iamFeignClient;

    @Mock
    private FileSaasScopeDao scopeDao;

    @InjectMocks
    private FileSaasAccessService service;

    @Test
    void shouldRequireLiveTrustedPermissionAndMappedTenant() {
        SaasIamContextVO context = new SaasIamContextVO("user", "tenant", "member", "app", 2L, 7L);
        when(iamFeignClient.checkApiPermission(eq("subject-token"), any(SaasIamPermissionCheckDTO.class)))
                .thenReturn(new SaasIamPermissionDecisionVO(true, context));
        when(scopeDao.findEnabledTenantBusinessId("tenant")).thenReturn("TENANT_00000001");
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getTokenValue).thenReturn("subject-token");
            FileSaasScope actual = service.requireCurrent("POST:/api/file/internal/upload/check");
            assertThat(actual.tenantRefId()).isEqualTo("tenant");
            assertThat(actual.appRefId()).isEqualTo("app");
            assertThat(actual.userRefId()).isEqualTo("user");
            assertThat(actual.memberRefId()).isEqualTo("member");
            assertThat(actual.tenantId()).isEqualTo("TENANT_00000001");
            verify(iamFeignClient).checkApiPermission(eq("subject-token"),
                    any(SaasIamPermissionCheckDTO.class));
        }
    }

    @Test
    void shouldRejectMissingUserTokenWithoutCallingRemote() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getTokenValue).thenReturn(null);
            assertThatThrownBy(() -> service.requireCurrent("POST:/api/file/internal/upload"))
                    .isInstanceOf(SecurityException.class);
        }
        verifyNoInteractions(iamFeignClient, scopeDao);
    }

    @Test
    void shouldRejectDeniedApiActionWithoutResolvingTenant() {
        when(iamFeignClient.checkApiPermission(eq("subject-token"), any(SaasIamPermissionCheckDTO.class)))
                .thenReturn(new SaasIamPermissionDecisionVO(false));
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getTokenValue).thenReturn("subject-token");
            assertThatThrownBy(() -> service.requireCurrent("GET:/api/file/internal/FILE0001"))
                    .isInstanceOf(SecurityException.class);
        }
        verifyNoInteractions(scopeDao);
    }

    @Test
    void shouldRejectMissingPhysicalOwnerOrTenantMapping() {
        SaasIamContextVO context = new SaasIamContextVO("user", "tenant", "member", "app", 2L, 7L);
        when(iamFeignClient.requireContext("subject-token")).thenReturn(context);
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getTokenValue).thenReturn("subject-token");
            assertThatThrownBy(service::requireContext).isInstanceOf(SecurityException.class);
        }
    }

    @Test
    void shouldRejectPathSegmentsOutsideBusinessIdGrammar() {
        assertThatThrownBy(() -> service.requireForFile("DELETE", "../OTHER", ""))
                .isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> service.requireForFile("GET", "FILE1", "/unknown"))
                .isInstanceOf(SecurityException.class);
        verifyNoInteractions(iamFeignClient, scopeDao);
    }
}
