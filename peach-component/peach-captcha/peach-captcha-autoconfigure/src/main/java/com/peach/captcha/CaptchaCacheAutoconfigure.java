package com.peach.captcha;

import com.peach.captcha.factory.CaptchaServiceFactory;
import com.peach.captcha.constant.CaptchaEnum;
import com.peach.captcha.service.impl.cache.MemoryCaptchaCacheService;
import com.peach.captcha.service.impl.cache.RedisCaptchaCacheService;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
    @ConditionalOnMissingBean(CaptchaCacheService.class)
    public CaptchaCacheService captchaCacheService(CaptchaProperties captchaProperties, BeanFactory beanFactory) {
        CaptchaEnum.CaptchaCacheType type = captchaProperties.getCacheType();
        if (type == CaptchaEnum.CaptchaCacheType.MEMORY) {
            return new MemoryCaptchaCacheService(Integer.parseInt(captchaProperties.getCacheNumber()));
        }
        if (type == CaptchaEnum.CaptchaCacheType.REDIS) {
            return new RedisCaptchaCacheService(beanFactory.getBean(StringRedisTemplate.class));
        }
        return CaptchaServiceFactory.getCaptchaCacheService(type.getCode());
    }


}
