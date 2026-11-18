package com.peach.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import org.springframework.stereotype.Component;

/**
 * 仅供通过完整凭证验证后的邀请接受流程创建一次性全局主体。
 *
 * <p>不向调用方返回 Token，不装配 SaaS 成员上下文；
 * 业务完成后立即注销，正式用户 Token 仍由正常租户登录流程创建。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 19:45
 */
@Component
public class AuthInvitationTokenSession {

    /**
     * 创建仅在当前请求内使用的全局主体。
     */
    public void open(String businessUserId) {
        StpUtil.login(businessUserId,
                new SaLoginParameter().setIsConcurrent(true).setIsShare(false));
    }

    /**
     * 注销当前临时全局主体。
     */
    public void close() {
        StpUtil.logout();
    }
}
