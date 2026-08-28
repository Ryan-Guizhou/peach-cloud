package com.peach.captcha;

import com.peach.captcha.factory.CaptchaServiceFactory;
import com.peach.captcha.service.CaptchaCacheService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * CaptchaCacheAutoconfigure相关类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:41
 */
@AutoConfiguration
public class CaptchaCacheAutoconfigure {

    @Bean
    public CaptchaCacheService captchaCacheService(CaptchaProperties captchaProperties) {
        return CaptchaServiceFactory.getCaptchaCacheService(captchaProperties.getCacheType().getCode());
    }


}
