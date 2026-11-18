package com.peach.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.peach.auth.dao.TenantMemberDao;
import com.peach.auth.service.TenantMembershipPolicy;
import com.peach.auth.service.ScopedPermissionService;
import com.peach.auth.service.support.IamPermissionCodes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 使用服务端 Sa-Token 主体校验全局用户与成员操作身份。
 *
 * <p>当前 Token 登录主键为永久 USER_ID，映射到用户物理 ID 时必须再次查询数据库。
 * 管理行为必须要求当前 Token 的物理成员、租户、应用与一致，并从同一角色绑定
 * 取得完整租户范围的固定权限。没有有效权限定义或绑定时一律拒绝。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 16:50
 */
@Service
@RequiredArgsConstructor
public class SaTokenTenantMembershipPolicy implements TenantMembershipPolicy {

    private final TenantMemberDao tenantMemberDao;

    private final TenantSessionService tenantSessionService;

    private final ScopedPermissionService scopedPermissionService;

    @Override
    public void requireCurrentUser(String userRefId) {
        if (!StringUtils.hasText(userRefId) || !StpUtil.isLogin()) {
            throw new SecurityException("Authenticated user identity is required");
        }
        String businessUserId = StpUtil.getLoginIdAsString();
        if (!StringUtils.hasText(businessUserId)
                || tenantMemberDao.countAuthenticatedUserIdentity(businessUserId, userRefId) != 1) {
            throw new SecurityException("Authenticated user identity does not match");
        }
    }

    @Override
    public void requireTenantAdministrator(String tenantRefId, String operatorMemberRefId) {
        requireTenantPermission(tenantRefId, operatorMemberRefId,
                IamPermissionCodes.TENANT_MEMBER_MANAGE);
    }

    @Override
    public void requireTenantPermission(String tenantRefId, String operatorMemberRefId,
                                        String permissionCode) {
        if (!StringUtils.hasText(tenantRefId) || !StringUtils.hasText(operatorMemberRefId)
                || !StringUtils.hasText(permissionCode)) {
            throw new SecurityException("Tenant permission identity and action are required");
        }
        TenantSessionContext context = tenantSessionService.requireCurrent();
        if (!tenantRefId.equals(context.tenantRefId())
                || !operatorMemberRefId.equals(context.memberRefId())
                || !scopedPermissionService.hasTenantPermission(
                        context.tenantRefId(), context.memberRefId(), context.appRefId(),
                        permissionCode)) {
            throw new SecurityException("Tenant action is not authorized");
        }
    }
}
