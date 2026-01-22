package com.peach.captcha.factory;

import com.peach.captcha.service.CaptchaCacheService;
import com.peach.captcha.constant.CaptchaConst;
import com.peach.captcha.service.CaptchaService;
import com.peach.captcha.provider.CaptchaCacheProvider;
import com.peach.captcha.provider.CaptchaServiceProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Slf4j
public class CaptchaServiceFactory {

    public static final Map<String, CaptchaCacheService> PROVIDERS = new ConcurrentHashMap<>();

    public static final Map<String, CaptchaService> INSTANCES = new ConcurrentHashMap<>();

    static {
        ServiceLoader<CaptchaCacheProvider> cacheProvider = ServiceLoader.load(CaptchaCacheProvider.class);
        for (CaptchaCacheProvider provider : cacheProvider) {
            PROVIDERS.put(provider.type(), provider.createCaptchaCacheService());
            log.info("Captcha autoconfig loaded captcha cache provider: [{}]", provider.type());
        }

        ServiceLoader<CaptchaServiceProvider> captchaProvider = ServiceLoader.load(CaptchaServiceProvider.class);
        for (CaptchaServiceProvider provider : captchaProvider) {
            INSTANCES.put(provider.type(), provider.createCaptchaService());
            log.info("Captcha autoconfig loaded captcha provider: [{}]", provider.type());
        }
    }

    /**
     * 获取缓存服务
     * @param type 缓存类型
     * @return 缓存服务
     */
    public static CaptchaCacheService getCaptchaCacheService(String type) {
        CaptchaCacheService captchaCacheService = PROVIDERS.get(type);
        if (captchaCacheService == null) {
            log.error("Unsupported captcha cache type: [{}]", type);
            throw new IllegalArgumentException("Unsupported captcha cache type: " + type);
        }
        return captchaCacheService;
    }

    /**
     * 获取验证码服务
     * @param type 验证码类型
     * @return 验证码服务
     */
    public static CaptchaService getCaptchaService(Properties config) {
        String captchaType = config.getProperty(CaptchaConst.CAPTCHA_TYPE, CaptchaConst.DEFAULT_CAPTCHA_TYPE);
        CaptchaService captchaService = INSTANCES.get(captchaType);
        if (captchaService == null) {
            log.error("Unsupported captcha type: [{}]", captchaType);
            throw new IllegalArgumentException("Unsupported captcha type: " + captchaType);
        }
        captchaService.init(config);
        return captchaService;
    }


}
