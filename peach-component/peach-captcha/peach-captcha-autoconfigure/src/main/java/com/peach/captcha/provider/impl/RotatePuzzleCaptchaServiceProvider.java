package com.peach.captcha.provider.impl;

import com.peach.captcha.constant.CaptchaEnum;
import com.peach.captcha.provider.CaptchaServiceProvider;
import com.peach.captcha.service.CaptchaService;
import com.peach.captcha.service.impl.RotatePuzzleCaptchaServiceImpl;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/22 11:38
 */
public class RotatePuzzleCaptchaServiceProvider implements CaptchaServiceProvider {

    @Override
    public String type() {
        return CaptchaEnum.CaptchaServiceType.ROTATEPUZZLE.getCode();
    }

    @Override
    public CaptchaService createCaptchaService() {
        return new RotatePuzzleCaptchaServiceImpl();
    }
}
