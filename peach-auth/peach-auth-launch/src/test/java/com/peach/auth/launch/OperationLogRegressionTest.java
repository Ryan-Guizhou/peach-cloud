package com.peach.auth.launch;

import com.peach.log.annoation.UserOperLog;
import com.peach.log.core.*;
import com.peach.log.interceptor.UserOperLogInterceptor;
import com.peach.log.vo.UserOperLogVO;
import com.peach.common.response.Response;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * 操作日志必填映射、异常记录与失败批次保留回归。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 21:00
 */
class OperationLogRegressionTest {
    @BeforeEach void context() {
        var actor = new com.peach.satoken.context.UserContext();
        actor.setTenantId("tenant"); actor.setOrgId("org"); actor.setUserCode("fixture-user");
        com.peach.satoken.context.SecurityContextHolder.set(actor);
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(new org.springframework.web.context.request.ServletRequestAttributes(new org.springframework.mock.web.MockHttpServletRequest("POST", "/fixture")));
    }
    @AfterEach void clear() {
        UserOperLogQueue.getInstance().getAllUserOperLog();
        com.peach.satoken.context.SecurityContextHolder.clear();
        org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
    }
    @UserOperLog public Response fixture() { return Response.success(); }

    @Test void mapperInsertIncludesMandatoryTenantAndOrganisation() throws Exception {
        String resource = "com/peach/userservice/dao/UserOperLogDao.xml";
        var config = new Configuration();
        try (var stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(stream, config, resource, config.getSqlFragments()).parse();
        }
        String sql = config.getMappedStatement("com.peach.log.dao.UserOperLogDao.insert").getBoundSql(new UserOperLogVO()).getSql();
        assertTrue(sql.contains("TENANT_ID")); assertTrue(sql.contains("ORG_ID"));
    }
    @Test void failedBatchRemainsAvailableForRetry() {
        UserOperLogQueue.getInstance().getAllUserOperLog();
        var service = mock(IUserOperLogService.class);
        doThrow(new IllegalStateException("temporary storage failure")).doNothing().when(service).batchInsert(anyList());
        var task = new UserOperLogTask(service);
        var record = new UserOperLogVO(); record.setId("retry-fixture");
        UserOperLogQueue.getInstance().addOperLogQueue(record);
        assertDoesNotThrow(task::handleUserOperLog);
        assertDoesNotThrow(task::handleUserOperLog);
        verify(service, times(2)).batchInsert(argThat(batch -> batch.size() == 1 && "retry-fixture".equals(batch.getFirst().getId())));
    }
    @Test void exceptionalBusinessResultIsStillRecordedWithoutReplacingException() throws Throwable {
        var invocation = mock(MethodInvocation.class);
        when(invocation.getMethod()).thenReturn(getClass().getMethod("fixture"));
        var original = new IllegalStateException("business failure");
        when(invocation.proceed()).thenThrow(original);
        assertSame(original, assertThrows(IllegalStateException.class, () -> new UserOperLogInterceptor().invoke(invocation)));
        List<UserOperLogVO> records = UserOperLogQueue.getInstance().getAllUserOperLog();
        assertEquals(1, records.size()); assertEquals("N", records.getFirst().getIsSuccess());
    }
}
