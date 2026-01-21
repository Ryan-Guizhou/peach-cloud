package com.peach.captcha.provider;

import com.peach.captcha.service.CaptchaCacheService;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:53
 */
public interface CaptchaCacheProvider {

    /**
     * 缓存类型
     * @return
     */
    String type();

    /**
     * 创建缓存服务
     * @return
     */
    CaptchaCacheService createCaptchaCacheService();
}
