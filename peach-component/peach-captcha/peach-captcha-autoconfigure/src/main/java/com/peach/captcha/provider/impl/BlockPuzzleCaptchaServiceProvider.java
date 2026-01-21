package com.peach.captcha.provider.impl;

import com.peach.captcha.constant.CaptchaEnum;
import com.peach.captcha.provider.CaptchaServiceProvider;
import com.peach.captcha.service.CaptchaService;
import com.peach.captcha.service.impl.BlockPuzzleCaptchaServiceImpl;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/21 18:21
 */
public class BlockPuzzleCaptchaServiceProvider implements CaptchaServiceProvider {

    @Override
    public String type() {
        return CaptchaEnum.CaptchaServiceType.BLOCKPUZZLE.getCode();
    }

    @Override
    public CaptchaService createCaptchaService() {
        return new BlockPuzzleCaptchaServiceImpl();
    }
}
