package com.peach.redis.tool.quickstart;

import com.peach.redis.tool.RedisDao;
import com.peach.redis.tool.quickstart.config.RedisToolDemoContext;
import com.peach.redis.tool.quickstart.config.RedisToolDemoProperties;
import com.peach.redis.tool.quickstart.enums.RedisToolKey;
import com.peach.redis.tool.quickstart.example.RedisToolOperationsExample;
import com.peach.redis.tool.quickstart.example.SessionStringExample;
import com.peach.redis.tool.quickstart.example.UserProfileHashExample;
import com.peach.redis.tool.quickstart.example.UserTagSetExample;
import com.peach.redis.tool.quickstart.runner.RedisToolDemoRunner;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * 验证前置失败停止业务、后置失败保留原始异常，以及清理范围隔离。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/12 16:00
 */
class RedisToolDemoLifecycleTest {
    @Test
    void preCleanupFailureMustStopBusiness() throws Exception {
        RedisDao dao = mock(RedisDao.class);
        RedisToolDemoProperties properties = new RedisToolDemoProperties();
        RedisToolDemoContext context = new RedisToolDemoContext(properties, dao);
        RedisToolOperationsExample operations = mock(RedisToolOperationsExample.class);
        SessionStringExample sessions = mock(SessionStringExample.class);
        when(dao.delete(anyString())).thenThrow(new IllegalStateException("cleanup failed"));
        RedisToolDemoRunner runner = runner(context, properties, operations, sessions);
        assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments())).hasMessage("cleanup failed");
        verify(operations).prepare();
        verify(operations, never()).run();
        verifyNoInteractions(sessions);
    }

    @Test
    void cleanupMustNotMaskBusinessFailure() throws Exception {
        RedisDao dao = mock(RedisDao.class);
        RedisToolDemoProperties properties = new RedisToolDemoProperties();
        properties.setCleanupAfterRun(true);
        RedisToolDemoContext context = new RedisToolDemoContext(properties, dao);
        RedisToolOperationsExample operations = mock(RedisToolOperationsExample.class);
        SessionStringExample sessions = mock(SessionStringExample.class);
        RuntimeException business = new IllegalStateException("business failed");
        RuntimeException cleanup = new IllegalStateException("cleanup failed");
        String session = context.key(RedisToolKey.SESSION, "s-1001");
        when(dao.delete(session)).thenReturn(false).thenThrow(cleanup);
        doThrow(business).when(operations).run();
        assertThatThrownBy(() -> runner(context, properties, operations, sessions)
                .run(new DefaultApplicationArguments())).isSameAs(business);
        assertThat(business.getSuppressed()).containsExactly(cleanup);
    }

    @Test
    void contextMustUseDefinitionsAndRejectPatternsAsDataKeys() {
        RedisDao dao = mock(RedisDao.class);
        RedisToolDemoProperties properties = new RedisToolDemoProperties();
        properties.setRunId("local-demo");
        RedisToolDemoContext context = new RedisToolDemoContext(properties, dao);
        String key = context.key(RedisToolKey.PROFILE, "u-1");
        assertThat(key).isEqualTo("peach:quickstart:tool:local-demo:profile:u-1");
        context.clear();
        verify(dao).delete(key);
        verifyNoMoreInteractions(dao);
        assertThatThrownBy(() -> context.key(RedisToolKey.DELETE_PATTERN, "all"))
                .isInstanceOf(IllegalArgumentException.class);
        properties.setRunId("*");
        assertThatThrownBy(() -> new RedisToolDemoContext(properties, dao))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private RedisToolDemoRunner runner(RedisToolDemoContext context, RedisToolDemoProperties properties,
                                      RedisToolOperationsExample operations, SessionStringExample sessions) {
        UserProfileHashExample profiles = mock(UserProfileHashExample.class);
        UserTagSetExample tags = mock(UserTagSetExample.class);
        when(profiles.getAll(anyString())).thenReturn(java.util.Map.of());
        when(tags.listTags(anyString())).thenReturn(java.util.Set.of());
        return new RedisToolDemoRunner(sessions, profiles, tags, operations, context, properties);
    }
}
