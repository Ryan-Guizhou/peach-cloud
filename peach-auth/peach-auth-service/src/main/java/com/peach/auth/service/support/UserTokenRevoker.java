package com.peach.auth.service.support;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 在密码更新事务提交之后回收一个全局账号的所有 Sa-Token 终端凭证。
 *
 * <p>Redis 失败不允许恢复旧 Token 的权限；调用方同时依赖数据库
 * PASSWORD_VERSION 在每次 SaaS 认证时作最终失效判定。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/19 11:00
 */
@Component
public class UserTokenRevoker {

    /**
     * 注销全局业务用户号下的全部设备 Token。
     *
     * @param businessUserId 当前数据库中已验证的永久业务用户 ID
     */
    public void revokeAll(String businessUserId) {
        if (!StringUtils.hasText(businessUserId)) {
            throw new SecurityException("Global user identity is required for token revocation");
        }
        StpUtil.logout(businessUserId);
    }
}
