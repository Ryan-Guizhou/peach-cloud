package com.peach.auth.rest.external;

import cn.dev33.satoken.same.SaSameUtil;
import com.peach.auth.service.IRoleService;
import com.peach.auth.service.IRouterService;
import com.peach.auth.vo.RoleVO;
import com.peach.auth.vo.RouterVO;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 旧 external 目录必须先验证服务身份，才允许通过内部 Feign 查询。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/20 15:10
 */
class LegacyExternalDirectoryControllerTest {

    @Test
    void shouldRejectUntrustedServicesBeforeReadingAnyGlobalDirectory() {
        IRoleService roles = mock(IRoleService.class);
        IRouterService routers = mock(IRouterService.class);
        RoleExternalController roleController = new RoleExternalController(roles);
        RouterExternalController routerController = new RouterExternalController(routers);

        try (MockedStatic<SaSameUtil> same = mockStatic(SaSameUtil.class)) {
            same.when(SaSameUtil::checkCurrentRequestToken)
                    .thenThrow(new SecurityException("Same-Token required"));

            assertThatThrownBy(() -> roleController.selectById("role-ref"))
                    .isInstanceOf(SecurityException.class);
            assertThatThrownBy(() -> routerController.selectById("router-ref"))
                    .isInstanceOf(SecurityException.class);
        }
        verifyNoInteractions(roles, routers);
    }

    @Test
    void shouldKeepTrustedInternalFeignLookupAvailable() {
        IRoleService roles = mock(IRoleService.class);
        IRouterService routers = mock(IRouterService.class);
        RoleVO role = new RoleVO();
        RouterVO router = new RouterVO();
        when(roles.selectById("role-ref")).thenReturn(role);
        when(routers.selectById("router-ref")).thenReturn(router);

        try (MockedStatic<SaSameUtil> same = mockStatic(SaSameUtil.class)) {
            new RoleExternalController(roles).selectById("role-ref");
            new RouterExternalController(routers).selectById("router-ref");
            same.verify(SaSameUtil::checkCurrentRequestToken, times(2));
        }
        verify(roles).selectById("role-ref");
        verify(routers).selectById("router-ref");
    }
}
