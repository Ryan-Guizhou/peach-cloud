package com.peach.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.peach.auth.dao.TenantMemberDao;
import com.peach.auth.entity.TenantMemberDO;
import com.peach.auth.service.TenantMembershipPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 使用服务端 Sa-Token 主体校验全局用户与成员操作身份。
 *
 * <p>当前 Token 登录主键为永久 USER_ID，映射到用户物理 ID 时必须再次查询数据库。
 * 租户管理员 RBAC 尚未完成，任何管理员变更一律拒绝，不从机构归属或请求参数推断授权。
 * 后续统一 Token 主体模型时需同步调整此映射和授权逻辑。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 16:50
 */
@Service
@RequiredArgsConstructor
public class SaTokenTenantMembershipPolicy implements TenantMembershipPolicy {

    private final TenantMemberDao tenantMemberDao;

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
        if (!StringUtils.hasText(tenantRefId) || !StringUtils.hasText(operatorMemberRefId)) {
            throw new SecurityException("Tenant administration scope is required");
        }
        TenantMemberDO member = tenantMemberDao.selectByIdInTenant(operatorMemberRefId, tenantRefId);
        if (member == null || !Integer.valueOf(0).equals(member.getIsDelete())
                || !"ACTIVE".equals(member.getStatus())) {
            throw new SecurityException("Administrator membership is not active");
        }
        requireCurrentUser(member.getUserRefId());
        // 真正的角色与资源授权由 RBAC 实现；在此之前不得根据机构归属授予管理权限。
        throw new SecurityException("Tenant administration RBAC is not configured");
    }
}
