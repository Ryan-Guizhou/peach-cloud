package com.peach.captcha.provider.impl;

import com.peach.captcha.constant.CaptchaEnum;
import com.peach.captcha.provider.CaptchaServiceProvider;
import com.peach.captcha.service.CaptchaService;
import com.peach.captcha.service.impl.TextCaptchaServiceImpl;

/**
 * 文本/运算验证码服务提供者
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/22 14:20
 */
public class TextCaptchaServiceProvider implements CaptchaServiceProvider {

    @Override
    public String type() {
        return CaptchaEnum.CaptchaServiceType.TEXT.getCode();
    }

    @Override
    public CaptchaService createCaptchaService() {
        return new TextCaptchaServiceImpl();
    }
}
