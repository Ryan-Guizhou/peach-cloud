package com.peach.captcha.provider.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.peach.captcha.service.CaptchaCacheService;
import com.peach.captcha.constant.CaptchaEnum;
import com.peach.captcha.provider.CaptchaCacheProvider;
import com.peach.captcha.service.impl.redis.RedisCaptchaCacheService;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:57
 */
public class RedisCaptchaCacheProvider implements CaptchaCacheProvider {

    @Override
    public String type() {
        return CaptchaEnum.CaptchaCacheType.REDIS.getCode();
    }

    @Override
    public CaptchaCacheService createCaptchaCacheService() {
        return new RedisCaptchaCacheService(SpringUtil.getBean(StringRedisTemplate.class));
    }
}
