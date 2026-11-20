package com.peach.auth.rest.internal;

import com.peach.auth.annoation.UserOperLog;
import com.peach.auth.core.UserOperLogQueue;
import com.peach.auth.enums.UserLogEnum;
import com.peach.auth.interceptor.UserOperLogInterceptor;
import com.peach.auth.util.TransferUtil;
import com.peach.auth.util.UserOperLogTrustedContext;
import com.peach.auth.vo.UserOperLogVO;
import com.peach.common.response.Response;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 旧操作审计不得持久化密码、Token、敏感响应或未经验证的租户身份。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/20 13:50
 */
class UserOperationAuditSafetyTest {

    @AfterEach
    void cleanUp() {
        RequestContextHolder.resetRequestAttributes();
        UserOperLogQueue.getInstance().getAllUserOperLog();
    }

    @Test
    void shouldCaptureTrustedSaasPhysicalScopeWithoutRequestOrResponseSecrets() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/user/add");
        request.setAttribute(UserOperLogTrustedContext.TENANT_REF_ID, "trusted-tenant");
        request.setAttribute(UserOperLogTrustedContext.APP_REF_ID, "trusted-app");
        request.setAttribute(UserOperLogTrustedContext.MEMBER_REF_ID, "trusted-member");
        request.addHeader(UserOperLogTrustedContext.TENANT_REF_ID, "spoofed-tenant");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        UserOperLog definition = mock(UserOperLog.class);
        when(definition.moduleCode()).thenReturn(UserLogEnum.Module.USERSERVICE);
        when(definition.optType()).thenReturn(UserLogEnum.OptType.INSERT);
        when(definition.optLevel()).thenReturn(UserLogEnum.LogLevel.INFO);
        when(definition.optContent()).thenReturn("'Create user ['+#p0+']'");

        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getArguments()).thenReturn(new Object[] {
                Map.of("password", "SENSITIVE_PASSWORD", "email", "user@example.test")
        });
        UserOperLogVO record = TransferUtil.transferToOperLog(invocation,
                definition, 12, Response.success(Map.of("token", "SENSITIVE_TOKEN")));

        assertThat(record.getTenantRefId()).isEqualTo("trusted-tenant");
        assertThat(record.getAppRefId()).isEqualTo("trusted-app");
        assertThat(record.getMemberRefId()).isEqualTo("trusted-member");
        assertThat(record.getOptContent()).isEqualTo("USERSERVICE:INSERT");
        assertThat(record.getOptContent()).doesNotContain("SENSITIVE_PASSWORD");
        assertThat(record.getResponseData()).isNull();
        assertThat(record.getErrorMsg()).isNull();
    }

    @Test
    void shouldIgnoreSpoofedHeadersWithoutTrustedServletAttributes() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/menu/add");
        request.addHeader(UserOperLogTrustedContext.TENANT_REF_ID, "attacker-tenant");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        UserOperLog definition = mock(UserOperLog.class);
        when(definition.moduleCode()).thenReturn(UserLogEnum.Module.USERSERVICE);
        when(definition.optType()).thenReturn(UserLogEnum.OptType.UPDATE);
        when(definition.optLevel()).thenReturn(UserLogEnum.LogLevel.INFO);
        when(definition.optContent()).thenReturn("'Static safe action'");

        UserOperLogVO record = TransferUtil.transferToOperLog(mock(MethodInvocation.class),
                definition, 9, Response.fail("Sensitive failure message SENSITIVE_PASSWORD"));

        assertThat(record.getTenantRefId()).isNull();
        assertThat(record.getAppRefId()).isNull();
        assertThat(record.getMemberRefId()).isNull();
        assertThat(record.getErrorMsg()).isEqualTo("Operation failed");
        assertThat(record.getResponseData()).isNull();
        assertThat(record.getOptContent()).isEqualTo("Static safe action");
    }

    @Test
    void shouldRecordDeniedOperationWithoutChangingTheOriginalException() throws Throwable {
        UserOperLogQueue.getInstance().getAllUserOperLog();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                new MockHttpServletRequest("DELETE", "/auth/user/delById")));
        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getMethod()).thenReturn(AnnotatedActions.class.getMethod("deleteUser"));
        SecurityException original = new SecurityException("SENSITIVE_TOKEN");
        when(invocation.proceed()).thenThrow(original);

        assertThatThrownBy(() -> new UserOperLogInterceptor().invoke(invocation))
                .isSameAs(original);

        List<UserOperLogVO> recorded = UserOperLogQueue.getInstance().getAllUserOperLog();
        assertThat(recorded).hasSize(1);
        assertThat(recorded.get(0).getIsSuccess()).isEqualTo("false");
        assertThat(recorded.get(0).getErrorMsg()).isEqualTo("Operation failed");
        assertThat(recorded.get(0).getOptContent()).doesNotContain("SENSITIVE_TOKEN");
        assertThat(recorded.get(0).getResponseData()).isNull();
    }

    static class AnnotatedActions {
        @UserOperLog(moduleCode = UserLogEnum.Module.USERSERVICE,
                optType = UserLogEnum.OptType.DELETE,
                optLevel = UserLogEnum.LogLevel.ERROR,
                optContent = "'Delete user ['+#p0+']'")
        public Response deleteUser() {
            return Response.success();
        }
    }
}
