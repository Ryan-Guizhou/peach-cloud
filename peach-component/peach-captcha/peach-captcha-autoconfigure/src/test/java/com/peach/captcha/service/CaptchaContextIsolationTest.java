package com.peach.captcha.service;

import com.peach.captcha.constant.CaptchaEnum;
import com.peach.captcha.constant.CaptchaPropertiesConst;
import com.peach.captcha.factory.CaptchaServiceFactory;
import com.peach.captcha.service.impl.cache.MemoryCaptchaCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证每个 CaptchaService 配置与内存缓存相互隔离，且容量耗尽不清空已有凭证。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/11
 */
class CaptchaContextIsolationTest {

    @Test
    void shouldCreateIndependentServicesPerContextWithoutSharingSettings() {
        Properties firstConfig = config("first-watermark");
        Properties secondConfig = config("second-watermark");
        MemoryCaptchaCacheService firstCache = new MemoryCaptchaCacheService(2);
        MemoryCaptchaCacheService secondCache = new MemoryCaptchaCacheService(2);

        CaptchaService first = CaptchaServiceFactory.getCaptchaService(firstConfig, firstCache);
        CaptchaService second = CaptchaServiceFactory.getCaptchaService(secondConfig, secondCache);

        assertThat(first).isNotSameAs(second);
        firstCache.set("shared-key", "first", 60);
        secondCache.set("shared-key", "second", 60);
        assertThat(firstCache.getAndDelete("shared-key")).isEqualTo("first");
        assertThat(secondCache.get("shared-key")).isEqualTo("second");
        assertThat(ReflectionTestUtils.getField(first, "WATER_MARK")).isEqualTo("first-watermark");
        assertThat(ReflectionTestUtils.getField(second, "WATER_MARK")).isEqualTo("second-watermark");
    }

    @Test
    void capacityLimitMustRejectWithoutClearingActiveCredentials() {
        MemoryCaptchaCacheService cache = new MemoryCaptchaCacheService(1);
        cache.set("first", "code", 60);
        assertThatThrownBy(() -> cache.set("second", "other", 60))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("capacity");
        assertThat(cache.getAndDelete("first")).isEqualTo("code");
        assertThat(cache.getAndDelete("first")).isNull();
    }

    private static Properties config(String watermark) {
        Properties properties = new Properties();
        properties.setProperty(CaptchaPropertiesConst.CAPTCHA_TYPE,
                CaptchaEnum.CaptchaServiceType.TEXT.getCode());
        properties.setProperty(CaptchaPropertiesConst.CAPTCHA_CACHETYPE,
                CaptchaEnum.CaptchaCacheType.MEMORY.getCode());
        properties.setProperty(CaptchaPropertiesConst.CAPTCHA_WATER_MARK, watermark);
        properties.setProperty(CaptchaPropertiesConst.REQ_FREQUENCY_LIMIT_ENABLE, "0");
        return properties;
    }
}
