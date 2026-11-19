package com.peach.auth.service.impl;

import com.peach.auth.dao.TenantInvitationDao;
import com.peach.auth.dao.UserDao;
import com.peach.auth.entity.UserDO;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 已验证凭证后将本人邀请安全转化为目标租户成员关系。
 *
 * <p>必须在原有用户密码、验证码及登录锁保护全部通过后才能调用。
 * 临时全局 Token 不会返回客户端，绝不允许以它替代 SaaS 成员认证。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 19:45
 */
@Service
@RequiredArgsConstructor
public class AuthenticatedInvitationService {

    private final AuthInvitationTokenSession tokenSession;

    private final UserDao userDao;

    private final TenantInvitationDao invitationDao;

    private final TenantMemberLifecycleService lifecycleService;

    /**
     * 接受绑定到当前已验证全局用户的有效邀请。
     *
     * @param businessUserId 已认证账号的永久用户业务号
     * @param userRefId 已认证账号的 UUID 物理主键
     * @param invitationRefId 当前用户待接受邀请的物理主键
     * @param verifiedPasswordVersion 最初 BCrypt 验证时读取的全局密码版本
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void acceptAfterAuthentication(String businessUserId, String userRefId,
                                          String invitationRefId, long verifiedPasswordVersion) {
        if (!StringUtils.hasText(businessUserId) || !StringUtils.hasText(userRefId)
                || !StringUtils.hasText(invitationRefId) || invitationRefId.length() > 32) {
            throw new SecurityException("Verified invitation identity is required");
        }
        UserDO current = userDao.lockCredential(userRefId);
        if (current == null || !businessUserId.equals(current.getUserId())
                || current.getPasswordVersion() == null
                || verifiedPasswordVersion < 0
                || current.getPasswordVersion() != verifiedPasswordVersion) {
            throw new SecurityException("Password credential changed during invitation authentication");
        }
        boolean temporarySubjectCreated = false;
        try {
            tokenSession.open(businessUserId);
            temporarySubjectCreated = true;
            String tenantRefId = invitationDao.findTenantForVerifiedInvitee(
                    invitationRefId, userRefId);
            if (!StringUtils.hasText(tenantRefId)) {
                throw new SecurityException("Invitation does not belong to authenticated user");
            }
            lifecycleService.accept(tenantRefId, invitationRefId, userRefId);
        } finally {
            if (temporarySubjectCreated) {
                tokenSession.close();
            }
        }
    }
}
