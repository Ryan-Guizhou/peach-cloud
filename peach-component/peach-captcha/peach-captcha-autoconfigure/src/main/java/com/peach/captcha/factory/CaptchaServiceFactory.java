package com.peach.captcha.factory;

import com.peach.captcha.constant.CaptchaConst;
import com.peach.captcha.constant.CaptchaPropertiesConst;
import com.peach.captcha.provider.CaptchaCacheProvider;
import com.peach.captcha.provider.CaptchaServiceProvider;
import com.peach.captcha.service.CaptchaCacheService;
import com.peach.captcha.service.CaptchaService;
import com.peach.captcha.service.impl.AbstractCacheService;
import com.peach.common.loader.CustomServiceLoader;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Objects;

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

    protected static final Map<String, CaptchaCacheProvider> PROVIDERS = new ConcurrentHashMap<>();

    protected static final Map<String, CaptchaServiceProvider> INSTANCES = new ConcurrentHashMap<>();

    static {
        List<CaptchaCacheProvider> cacheProviders = CustomServiceLoader.load(CaptchaCacheProvider.class);
        for (CaptchaCacheProvider provider : cacheProviders) {
            try {
                PROVIDERS.put(provider.type(), provider);
                log.info("Captcha autoconfig loaded captcha cache provider: [{}]", provider.type());
            } catch (Exception | LinkageError ex) {
                // Redis 等可选 Provider：缺类/缺 Bean 时跳过；OOM 等 Error 仍向上抛。
                log.warn("Skip captcha cache provider [{}]: {}", provider.type(), ex.toString());
            }
        }

        List<CaptchaServiceProvider> captchaProviders = CustomServiceLoader.load(CaptchaServiceProvider.class);
        for (CaptchaServiceProvider provider : captchaProviders) {
            INSTANCES.put(provider.type(), provider);
            log.info("Captcha autoconfig loaded captcha provider: [{}]", provider.type());
        }
    }

    /**
     * 获取缓存服务
     * @param type 缓存类型
     * @return 缓存服务
     */
    public static CaptchaCacheService getCaptchaCacheService(String type) {
        CaptchaCacheProvider provider = PROVIDERS.get(type);
        if (provider == null) {
            throw new IllegalArgumentException("Unsupported captcha cache type: " + type);
        }
        return Objects.requireNonNull(provider.createCaptchaCacheService(),
                "Captcha cache provider returned null");
    }

    /**
     * 获取验证码服务
     *
     * @param config 验证码配置
     * @return 验证码服务
     */
    public static CaptchaService getCaptchaService(Properties config) {
        Objects.requireNonNull(config, "config");
        String cacheType = config.getProperty(CaptchaPropertiesConst.CAPTCHA_CACHETYPE, "MEMORY");
        return getCaptchaService(config, getCaptchaCacheService(cacheType));
    }

    /**
     * 每个 Spring 应用上下文创建新的服务实例，并绑定同一上下文的缓存 Bean。
     */
    public static CaptchaService getCaptchaService(Properties config, CaptchaCacheService cacheService) {
        Objects.requireNonNull(config, "config");
        String captchaType = config.getProperty(CaptchaConst.CAPTCHA_TYPE, CaptchaConst.DEFAULT_CAPTCHA_TYPE);
        CaptchaServiceProvider provider = INSTANCES.get(captchaType);
        if (provider == null) {
            throw new IllegalArgumentException("Unsupported captcha type: " + captchaType);
        }
        CaptchaService captchaService = Objects.requireNonNull(provider.createCaptchaService(),
                "Captcha service provider returned null");
        if (captchaService instanceof AbstractCacheService cacheAware) {
            cacheAware.bindCacheService(Objects.requireNonNull(cacheService, "cacheService"));
        }
        captchaService.init(config);
        return captchaService;
    }


}
