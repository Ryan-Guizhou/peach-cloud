package com.peach.auth.service.support;

import com.peach.auth.dao.UserOrgDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 在 Token 上下文写入后，使用独立的已提交读复核租户可用性。
 * 与停用后的会话撤销互补，避免停用扫描结束后才生成有效会话。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/21 18:00
 */
@Component
@RequiredArgsConstructor
public class TenantLoginAvailability {
    private final UserOrgDao userOrgDao;

    /**
     * 读取最新已提交的租户状态及有效机构绑定，不复用登录前的事务快照。
     * @param userId 用户 ID
     * @param tenantId 租户业务 ID
     * @param orgId 机构 ID
     * @return 当前上下文是否仍允许登录
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public boolean isAvailable(String userId, String tenantId, String orgId) {
        var bindings = userOrgDao.selectByUserIdAndTenantId(userId, tenantId);
        return bindings != null && bindings.stream().anyMatch(binding -> orgId.equals(binding.getOrgId()));
    }
}
