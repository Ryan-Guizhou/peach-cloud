package com.peach.captcha.provider.impl.cache;

import com.peach.captcha.constant.CaptchaEnum;
import com.peach.captcha.provider.CaptchaCacheProvider;
import com.peach.captcha.service.CaptchaCacheService;
import com.peach.captcha.service.impl.cache.MemoryCaptchaCacheService;

/**
 * Memory验证码缓存提供者。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:57
 */
public class MemoryCaptchaCacheProvider implements CaptchaCacheProvider {

    @Override
    public String type() {
        return CaptchaEnum.CaptchaCacheType.MEMORY.getCode();
    }

    @Override
    public CaptchaCacheService createCaptchaCacheService() {
        return new MemoryCaptchaCacheService();
    }
}
