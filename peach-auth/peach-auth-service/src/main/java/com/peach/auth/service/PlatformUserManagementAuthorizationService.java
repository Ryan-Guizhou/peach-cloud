package com.peach.auth.service;

import com.peach.auth.dao.TenantProvisioningDao;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 旧全局用户管理仅允许可信平台控制租户的受保护 SYSTEM 管理员使用。
 *
 * <p>该授权是全局用户数据的服务侧防线，不能用业务租户角色、请求参数或仅
 * API 菜单权限替代。沿用平台租户开通时的不可委派权限事实源；
 * 在单个事务中持有平台租户行锁并复核当前成员授权版本。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/20 10:20
 */
@Service
@RequiredArgsConstructor
public class PlatformUserManagementAuthorizationService {

    private final TenantSessionService tenantSessionService;

    private final TenantProvisioningDao provisioningDao;

    /**
     * 仅放行真实 PLATFORM 控制租户内持有不可委派 SYSTEM 权限的管理员。
     *
     * @throws SecurityException 非平台身份、无授权或授权版本过期时拒绝
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void requirePlatformAdministrator() {
        TenantSessionContext context = tenantSessionService.requireCurrent();
        if (!context.tenantRefId().equals(
                provisioningDao.lockPlatformTenant(context.tenantRefId()))
                || provisioningDao.countPlatformCreateGrant(
                context.tenantRefId(), context.memberRefId(), context.appRefId(),
                context.memberVersion(), context.authVersion()) < 1) {
            throw new SecurityException("Trusted platform administrator privilege is required");
        }
    }
}
