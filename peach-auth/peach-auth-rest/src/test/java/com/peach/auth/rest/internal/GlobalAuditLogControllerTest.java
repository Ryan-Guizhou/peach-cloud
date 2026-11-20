package com.peach.auth.rest.internal;

import com.peach.auth.core.IUserOperLogService;
import com.peach.auth.service.IAuthLogService;
import com.peach.auth.service.PlatformAdministrationAuthorizationService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 全局日志在查询数据库前必须校验可信 PLATFORM 系统管理员。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/20 12:10
 */
class GlobalAuditLogControllerTest {

    @Test
    void shouldDenyUntrustedSaasMembersBeforeAnyGlobalAuditLogQuery() {
        PlatformAdministrationAuthorizationService platformAccess =
                mock(PlatformAdministrationAuthorizationService.class);
        doThrow(new SecurityException("PLATFORM administrator required"))
                .when(platformAccess).requirePlatformAdministrator();

        IAuthLogService authService = mock(IAuthLogService.class);
        IUserOperLogService operationService = mock(IUserOperLogService.class);
        AuthLogController authLog = new AuthLogController(authService, platformAccess);
        UserOperLogController operations = new UserOperLogController(operationService, platformAccess);

        assertThatThrownBy(() -> authLog.pageList(null)).isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> operations.pageList(null)).isInstanceOf(SecurityException.class);
        verifyNoInteractions(authService, operationService);
    }
}
