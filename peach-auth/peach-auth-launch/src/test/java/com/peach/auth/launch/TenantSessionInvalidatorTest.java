package com.peach.auth.launch;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.peach.auth.dao.UserOrgDao;
import com.peach.auth.event.TenantUnavailableEvent;
import com.peach.auth.service.support.TenantSessionInvalidator;
import com.peach.common.constant.SaTokenConstant;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.List;
import static org.mockito.Mockito.*;

/**
 * 验证停用租户只撤销匹配上下文的会话。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/21 18:00
 */
class TenantSessionInvalidatorTest {
    @Test
    void postLoginCheckShouldRejectUnavailableTenantBinding() {
        var dao = mock(UserOrgDao.class);
        when(dao.selectByUserIdAndTenantId("user", "tenant-a")).thenReturn(List.of());
        var check = new com.peach.auth.service.support.TenantLoginAvailability(dao);
        org.assertj.core.api.Assertions.assertThat(check.isAvailable("user", "tenant-a", "org")).isFalse();
    }

    @Test
    void shouldPreserveOtherTenantSessions() {
        var dao = mock(UserOrgDao.class);
        var redis = mock(StringRedisTemplate.class);
        when(dao.selectUserIdsByTenantId("tenant-a")).thenReturn(List.of("user"));
        var matching = mock(SaSession.class);
        var other = mock(SaSession.class);
        when(matching.getString(SaTokenConstant.TOKEN_SESSION_TENANT_ID)).thenReturn("tenant-a");
        when(matching.getString(SaTokenConstant.TOKEN_SESSION_ORG_ID)).thenReturn("org");
        when(other.getString(SaTokenConstant.TOKEN_SESSION_TENANT_ID)).thenReturn("tenant-b");
        try (var stp = mockStatic(StpUtil.class)) {
            stp.when(() -> StpUtil.getTokenValueListByLoginId("user")).thenReturn(List.of("test-a", "test-b"));
            stp.when(() -> StpUtil.getTokenSessionByToken("test-a")).thenReturn(matching);
            stp.when(() -> StpUtil.getTokenSessionByToken("test-b")).thenReturn(other);
            new TenantSessionInvalidator(dao, redis).onTenantUnavailable(new TenantUnavailableEvent("tenant-a"));
            stp.verify(() -> StpUtil.logoutByTokenValue("test-a"));
            stp.verify(() -> StpUtil.logoutByTokenValue("test-b"), never());
            verify(redis).delete(SaTokenConstant.userApiResourceCacheKey("user", "tenant-a", "org"));
        }
    }
}
