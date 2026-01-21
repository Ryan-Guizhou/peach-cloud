package com.peach.captcha.provider;

import com.peach.captcha.service.CaptchaService;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 14:56
 */
public interface CaptchaServiceProvider {

    /**
     * 获取验证码类型
     * @return 缓存类型
     */
    String type();

    /**
     * 创建验证码服务
     * @return 缓存服务
     */
    CaptchaService createCaptchaService();
}
