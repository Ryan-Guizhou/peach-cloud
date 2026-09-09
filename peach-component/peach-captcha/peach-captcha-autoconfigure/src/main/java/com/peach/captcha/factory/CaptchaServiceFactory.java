package com.peach.captcha.factory;

import com.peach.captcha.constant.CaptchaConst;
import com.peach.captcha.provider.CaptchaCacheProvider;
import com.peach.captcha.provider.CaptchaServiceProvider;
import com.peach.captcha.service.CaptchaCacheService;
import com.peach.captcha.service.CaptchaService;
import com.peach.common.loader.CustomServiceLoader;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 验证码服务工厂。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Slf4j
public class CaptchaServiceFactory {

    private CaptchaServiceFactory() {
        throw new IllegalStateException("Utility class");
    }

    protected static final Map<String, CaptchaCacheService> PROVIDERS = new ConcurrentHashMap<>();

    protected static final Map<String, CaptchaService> INSTANCES = new ConcurrentHashMap<>();

    static {
        List<CaptchaCacheProvider> cacheProviders = CustomServiceLoader.load(CaptchaCacheProvider.class);
        for (CaptchaCacheProvider provider : cacheProviders) {
            PROVIDERS.put(provider.type(), provider.createCaptchaCacheService());
            log.info("Captcha autoconfig loaded captcha cache provider: [{}]", provider.type());
        }

        List<CaptchaServiceProvider> captchaProviders = CustomServiceLoader.load(CaptchaServiceProvider.class);
        for (CaptchaServiceProvider provider : captchaProviders) {
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
     *
     * @param config 验证码配置
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
