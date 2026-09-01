package com.peach.auth.service;

import com.peach.auth.vo.LoginLockStatusVO;

/**
 * 登录密码错误锁定服务。
 */
public interface LoginLockService {

    /**
     * 检查账号是否处于锁定状态。
     *
     * @param account 登录账号（用户名/手机号/邮箱）
     * @return 锁定状态
     */
    LoginLockStatusVO checkLock(String account);

    /**
     * 记录一次密码校验失败并返回最新锁定状态。
     *
     * @param account 登录账号
     * @return 锁定状态
     */
    LoginLockStatusVO recordFailure(String account);

    /**
     * 登录成功后清除失败计数与锁定状态。
     *
     * @param account 登录账号
     */
    void clearOnSuccess(String account);
}
