package com.peach.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.peach.auth.dao.TenantMemberDao;
import com.peach.auth.entity.TenantMemberDO;
import com.peach.common.constant.SaTokenConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * 认证后绑定并验证基于成员物理 ID 的 Token 专属 SaaS 上下文。
 *
 * <p>登录密码必须由调用方先验证，再调用 bindAfterAuthentication。
 * 切换租户时不得从请求中相信用户或成员 ID，始终以 Sa-Token 登录主体及数据库成员关系为准。
 * 请求校验每次检查成员 VERSION、租户/用户和应用有效性；成员停用或退出立即使旧上下文失效。
 * 角色授权版本检查仍需要 #206/#208 补齐，暂不将本服务等同于完整业务权限判定。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 17:35
 */
@Service
@RequiredArgsConstructor
public class TenantSessionService {

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final TenantMemberDao tenantMemberDao;
    private final TenantMemberLifecycleService membershipService;

    /**
     * 将服务端已完成密码认证的全局用户身份绑定至有效成员/应用。
     * 调用前必须完成 StpUtil.login，缺少匹配身份时拒绝写入任何租户上下文。
     *
     * @param verifiedUserRefId 已验证的用户物理 ID
     * @param tenantRefId 请求选择的租户物理 ID
     * @param appRefId 请求选择的应用物理 ID
     * @return 经服务端验证的上下文
     */
    @Transactional(readOnly = true)
    public TenantSessionContext bindAfterAuthentication(String verifiedUserRefId,
                                                         String tenantRefId, String appRefId) {
        requireIdentity(verifiedUserRefId);
        requireIdentity(tenantRefId);
        requireIdentity(appRefId);
        requireLoginUser(verifiedUserRefId);
        TenantMemberDO member = membershipService.resolveActive(tenantRefId, verifiedUserRefId);
        if (member == null || !Objects.equals(verifiedUserRefId, member.getUserRefId())
                || !Objects.equals(tenantRefId, member.getTenantRefId())) {
            throw new SecurityException("Active tenant membership is required");
        }
        if (tenantMemberDao.countEnabledApplication(tenantRefId, appRefId) != 1) {
            throw new SecurityException("Application is not enabled in the selected tenant");
        }
        Long version = member.getVersion();
        if (version == null || version < 0) {
            throw new SecurityException("Membership version is invalid");
        }
        TenantSessionContext context = new TenantSessionContext(verifiedUserRefId, tenantRefId,
                member.getId(), appRefId, version);
        var session = StpUtil.getTokenSession();
        session.set(SaTokenConstant.TOKEN_SESSION_USER_REF_ID, context.userRefId());
        session.set(SaTokenConstant.TOKEN_SESSION_TENANT_REF_ID, context.tenantRefId());
        session.set(SaTokenConstant.TOKEN_SESSION_MEMBER_REF_ID, context.memberRefId());
        session.set(SaTokenConstant.TOKEN_SESSION_APP_REF_ID, context.appRefId());
        session.set(SaTokenConstant.TOKEN_SESSION_MEMBER_VERSION, context.memberVersion());
        return context;
    }

    /**
     * 读取并在数据库中重新验证当前 Token 上下文。旧版本、禁用成员或跨租户上下文一律拒绝。
     *
     * @return 仍然有效的租户成员上下文
     */
    @Transactional(readOnly = true)
    public TenantSessionContext requireCurrent() {
        if (!StpUtil.isLogin()) {
            throw new SecurityException("Authenticated tenant session is required");
        }
        var session = StpUtil.getTokenSession();
        Object user = session.get(SaTokenConstant.TOKEN_SESSION_USER_REF_ID);
        Object tenant = session.get(SaTokenConstant.TOKEN_SESSION_TENANT_REF_ID);
        Object memberRef = session.get(SaTokenConstant.TOKEN_SESSION_MEMBER_REF_ID);
        Object app = session.get(SaTokenConstant.TOKEN_SESSION_APP_REF_ID);
        Object version = session.get(SaTokenConstant.TOKEN_SESSION_MEMBER_VERSION);
        if (!(user instanceof String userId) || !(tenant instanceof String tenantId)
                || !(memberRef instanceof String memberId) || !(app instanceof String appId)
                || !(version instanceof Number versionValue)) {
            throw new SecurityException("Tenant token is missing a trusted context");
        }
        TenantSessionContext context = new TenantSessionContext(
                userId, tenantId, memberId, appId, versionValue.longValue());
        requireLoginUser(context.userRefId());
        TenantMemberDO active = membershipService.resolveActive(context.tenantRefId(), context.userRefId());
        if (active == null || !Objects.equals(active.getId(), context.memberRefId())
                || !STATUS_ACTIVE.equals(active.getStatus())
                || !Objects.equals(active.getVersion(), context.memberVersion())
                || tenantMemberDao.countEnabledApplication(context.tenantRefId(), context.appRefId()) != 1) {
            throw new SecurityException("Tenant membership or application is no longer authorized");
        }
        return context;
    }

    /**
     * 当前已认证用户切换到另一个有效租户，不允许调用者更改用户物理主键。
     *
     * @param tenantRefId 目标租户物理 ID
     * @param appRefId 目标应用物理 ID
     * @return 新租户成员上下文
     */
    @Transactional(readOnly = true)
    public TenantSessionContext switchTenant(String tenantRefId, String appRefId) {
        TenantSessionContext current = requireCurrent();
        return bindAfterAuthentication(current.userRefId(), tenantRefId, appRefId);
    }

    private void requireLoginUser(String userRefId) {
        if (!StpUtil.isLogin()
                || tenantMemberDao.countAuthenticatedUserIdentity(
                        StpUtil.getLoginIdAsString(), userRefId) != 1) {
            throw new SecurityException("Token principal does not match current global user");
        }
    }

    private static void requireIdentity(String value) {
        if (!StringUtils.hasText(value)) {
            throw new SecurityException("Tenant session physical identifier is required");
        }
    }
}
