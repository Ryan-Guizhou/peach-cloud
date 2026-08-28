package com.peach.captcha;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * CaptchaAutoconfigure相关类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:41
 */
@AutoConfiguration
@EnableConfigurationProperties(CaptchaProperties.class)
@Import({CaptchaCacheAutoconfigure.class, CaptchaServiceAutoconfigure.class})
public class CaptchaAutoconfigure {


}
