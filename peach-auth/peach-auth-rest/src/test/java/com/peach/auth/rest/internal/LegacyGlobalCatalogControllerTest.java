package com.peach.auth.rest.internal;

import com.peach.auth.service.IFunctionService;
import com.peach.auth.service.IMenuService;
import com.peach.auth.service.IResouceService;
import com.peach.auth.service.IRouterService;
import com.peach.auth.service.PlatformAdministrationAuthorizationService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 旧全局目录所有读写均必须在平台身份检查失败后立即停止。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/20 11:40
 */
class LegacyGlobalCatalogControllerTest {

    @Test
    void shouldFailClosedForAllLegacyCatalogCrudMethods() {
        PlatformAdministrationAuthorizationService platformAccess =
                mock(PlatformAdministrationAuthorizationService.class);
        doThrow(new SecurityException("Platform authorization required"))
                .when(platformAccess).requirePlatformAdministrator();

        IMenuService menus = mock(IMenuService.class);
        IFunctionService functions = mock(IFunctionService.class);
        IResouceService resources = mock(IResouceService.class);
        IRouterService routers = mock(IRouterService.class);
        MenuController menu = new MenuController(menus, platformAccess);
        FunctionController function = new FunctionController(functions, platformAccess);
        ResourceController resource = new ResourceController(resources, platformAccess);
        RouterController router = new RouterController(routers, platformAccess);

        assertThatThrownBy(() -> menu.pageList(null)).isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> menu.selectById("menu")).isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> menu.add(null)).isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> menu.update(null)).isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> menu.delById("menu")).isInstanceOf(SecurityException.class);

        assertThatThrownBy(() -> function.pageList(null)).isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> function.selectById("function")).isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> function.add(null)).isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> function.update(null)).isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> function.delById("function")).isInstanceOf(SecurityException.class);

        assertThatThrownBy(() -> resource.pageList(null)).isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> resource.selectById("resource")).isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> resource.add(null)).isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> resource.update(null)).isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> resource.delById("resource")).isInstanceOf(SecurityException.class);

        assertThatThrownBy(() -> router.pageList(null)).isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> router.selectById("router")).isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> router.add(null)).isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> router.update(null)).isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> router.delById("router")).isInstanceOf(SecurityException.class);

        verifyNoInteractions(menus, functions, resources, routers);
    }
}
