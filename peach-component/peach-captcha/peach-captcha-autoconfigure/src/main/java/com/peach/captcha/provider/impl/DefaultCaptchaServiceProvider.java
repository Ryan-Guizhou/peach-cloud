package com.peach.captcha.provider.impl;

import com.peach.captcha.constant.CaptchaEnum;
import com.peach.captcha.service.CaptchaService;
import com.peach.captcha.provider.CaptchaServiceProvider;
import com.peach.captcha.service.impl.DefaultCaptchaService;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 15:53
 */
@Slf4j
public class DefaultCaptchaServiceProvider implements CaptchaServiceProvider {
    @Override
    public String type() {
        return CaptchaEnum.CaptchaServiceType.DEFAULT.getCode();
    }

    @Override
    public CaptchaService createCaptchaService() {
        return new DefaultCaptchaService();
    }
}
