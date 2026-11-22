package com.peach.auth.service.support;

import cn.dev33.satoken.stp.StpUtil;
import com.peach.auth.dao.UserOrgDao;
import com.peach.auth.event.TenantUnavailableEvent;
import com.peach.common.constant.SaTokenConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 在租户状态事务提交后，仅撤销该租户上下文中的 Token 和 API 授权缓存。
 * 同一用户在其他租户的会话保持有效，不记录 Token 内容。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/21 18:00
 */
@Component
@RequiredArgsConstructor
public class TenantSessionInvalidator {
    private final UserOrgDao userOrgDao;
    private final StringRedisTemplate redisTemplate;

    /**
     * 状态已提交后撤销相关会话；数据库事务回滚时不执行。
     * @param event 租户不可用事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTenantUnavailable(TenantUnavailableEvent event) {
        for (String userId : userOrgDao.selectUserIdsByTenantId(event.tenantId())) {
            var tokens = StpUtil.getTokenValueListByLoginId(userId);
            if (tokens == null) continue;
            for (String token : tokens) {
                var session = StpUtil.getTokenSessionByToken(token);
                if (session == null || !event.tenantId().equals(session.getString(SaTokenConstant.TOKEN_SESSION_TENANT_ID))) continue;
                String orgId = session.getString(SaTokenConstant.TOKEN_SESSION_ORG_ID);
                String fiscal = session.getString(SaTokenConstant.TOKEN_SESSION_FISCAL);
                StpUtil.logoutByTokenValue(token);
                redisTemplate.delete(SaTokenConstant.userApiResourceCacheKey(userId, event.tenantId(), orgId, fiscal));
            }
        }
    }
}
