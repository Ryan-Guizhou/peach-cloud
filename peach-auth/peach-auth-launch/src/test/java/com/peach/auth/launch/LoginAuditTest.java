package com.peach.auth.launch;

import com.peach.auth.dao.LoginLogDao;
import com.peach.auth.entity.LoginLogDO;
import com.peach.auth.qo.LoginLogQO;
import com.peach.auth.service.impl.LoginLogService;
import com.peach.auth.service.support.LoginAuditContext;
import com.peach.auth.service.support.LoginAuditProperties;
import com.peach.auth.service.support.LoginLogWriter;
import com.peach.common.exception.BusinessException;
import com.peach.satoken.context.SecurityContextHolder;
import com.peach.satoken.context.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 登录审计的数据最小化、租户隔离和写入故障回归。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 20:00
 */
class LoginAuditTest {
    @AfterEach void clear() { SecurityContextHolder.clear(); LoginAuditContext.clear(); }

    @Test void contextOnlyStoresMaskedAccountAndClearsBetweenRequests() {
        LoginAuditContext.begin("LOGIN", "127.0.0.1", "Chrome / Windows", "trace");
        LoginAuditContext.account("admin-account");
        LoginAuditContext.reason("BAD_CREDENTIALS");
        LoginLogDO record = LoginAuditContext.finish();
        assertFalse(record.getAccount().contains("admin-account"));
        assertEquals("FAILED", record.getResult());
        assertEquals("BAD_CREDENTIALS", record.getReasonCode());
        assertNotNull(record.getAccountHash());
        assertNull(record.getTenantId());
        LoginAuditContext.clear();
        assertNull(LoginAuditContext.finish());
    }

    @Test void rejectsUnassignedScopeWithoutSeparatePlatformPermission() {
        var dao = mock(LoginLogDao.class);
        var service = new LoginLogService(dao, new LoginAuditProperties());
        UserContext actor = new UserContext();
        actor.setTenantId("tenant"); actor.setUserId("user"); actor.setOrgId("org"); actor.setFiscal("2026");
        SecurityContextHolder.set(actor);
        when(dao.hasPermission(eq(actor), eq("loginLog:view"))).thenReturn(1);
        LoginLogQO query = new LoginLogQO(); query.setScope("UNASSIGNED");
        assertThrows(BusinessException.class, () -> service.pageList(query));
        verify(dao, never()).selectByQO(any());
    }

    @Test void recordDetailCannotCrossTenantEvenWithPlatformPermission() {
        var dao = mock(LoginLogDao.class);
        var service = new LoginLogService(dao, new LoginAuditProperties());
        UserContext actor = new UserContext(); actor.setTenantId("tenant"); actor.setUserId("user");
        SecurityContextHolder.set(actor);
        when(dao.hasPermission(eq(actor), eq("loginLog:view"))).thenReturn(1);
        LoginLogDO row = new LoginLogDO(); row.setTenantId("other"); row.setOccurredAt(LocalDateTime.now());
        when(dao.selectById("id")).thenReturn(row);
        assertThrows(BusinessException.class, () -> service.selectById("id"));
    }

    @Test void writerFailureNeverChangesAuthenticationResponseAndContextIsCleared() throws Exception {
        var writer = mock(LoginLogWriter.class);
        doThrow(new IllegalStateException("unavailable")).when(writer).save(any());
        var filter = new com.peach.auth.rest.audit.LoginAuditFilter(writer, new LoginAuditProperties());
        var request = new org.springframework.mock.web.MockHttpServletRequest("POST", "/auth/login");
        request.setRemoteAddr("192.0.2.10"); request.addHeader("X-Forwarded-For", "198.51.100.5");
        request.addHeader("User-Agent", "Mozilla Chrome/100 Windows");
        var response = new org.springframework.mock.web.MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> {
            LoginAuditContext.account("admin-account"); LoginAuditContext.success();
            res.getWriter().write("success");
        });
        assertEquals("success", response.getContentAsString());
        var captured = ArgumentCaptor.forClass(LoginLogDO.class);
        verify(writer).save(captured.capture());
        assertEquals("192.0.2.10", captured.getValue().getIpAddress());
        assertEquals("Chrome / Windows", captured.getValue().getBrowser());
        assertNull(LoginAuditContext.finish());
    }
    @Test void trustedProxyParsingStopsAtFirstUntrustedHop() {
        var properties = new LoginAuditProperties(); properties.setTrustedProxies(java.util.List.of("127.0.0.1"));
        var filter = new com.peach.auth.rest.audit.LoginAuditFilter(mock(LoginLogWriter.class), properties);
        var request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1"); request.addHeader("X-Forwarded-For", "198.51.100.1, 203.0.113.2");
        assertEquals("203.0.113.2", filter.sourceIp(request));
    }
    @Test void onlyFailedCaptchaEventsAreWritten() throws Exception {
        var writer = mock(LoginLogWriter.class);
        var filter = new com.peach.auth.rest.audit.LoginAuditFilter(writer, new LoginAuditProperties());
        var request = new org.springframework.mock.web.MockHttpServletRequest("POST", "/auth/checkCaptcha");
        filter.doFilter(request, new org.springframework.mock.web.MockHttpServletResponse(), (req, res) -> LoginAuditContext.success());
        verifyNoInteractions(writer);
        filter.doFilter(new org.springframework.mock.web.MockHttpServletRequest("POST", "/auth/checkCaptcha"), new org.springframework.mock.web.MockHttpServletResponse(), (req, res) -> { });
        verify(writer).save(argThat(row -> "CAPTCHA_FAILED".equals(row.getReasonCode()) && "FAILED".equals(row.getResult())));
    }
    @Test void genericExceptionIsRecordedWithoutExceptionTextAndRethrown() {
        var writer = mock(LoginLogWriter.class);
        var filter = new com.peach.auth.rest.audit.LoginAuditFilter(writer, new LoginAuditProperties());
        assertThrows(IllegalArgumentException.class, () -> filter.doFilter(
                new org.springframework.mock.web.MockHttpServletRequest("POST", "/auth/login"),
                new org.springframework.mock.web.MockHttpServletResponse(), (req, res) -> { throw new IllegalArgumentException("private input"); }));
        verify(writer).save(argThat(row -> "INTERNAL_ERROR".equals(row.getReasonCode())));
        assertNull(LoginAuditContext.finish());
    }
    @Test void accountDigestIsNotSerializedInResponses() throws Exception {
        LoginLogDO row = new LoginLogDO(); row.setAccountHash("internal-digest");
        String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(row);
        assertFalse(json.contains("accountHash")); assertFalse(json.contains("internal-digest"));
    }
}
