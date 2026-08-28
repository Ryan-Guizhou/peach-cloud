package com.peach.captcha;

import com.alibaba.fastjson.JSON;
import com.peach.captcha.constant.CaptchaPropertiesConst;
import com.peach.captcha.factory.CaptchaServiceFactory;
import com.peach.captcha.service.CaptchaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.Properties;

/**
 * CaptchaServiceAutoconfigure相关类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:41
 */
@Slf4j
@AutoConfiguration
public class CaptchaServiceAutoconfigure {

    @Bean
    @ConditionalOnMissingBean(CaptchaService.class)
    public CaptchaService captchaService(CaptchaProperties config){
        log.info("Custom configuration items: {}", JSON.toJSONString(config));
        Properties properties = new Properties();
        // 缓存类型
        properties.setProperty(CaptchaPropertiesConst.CAPTCHA_CACHETYPE, config.getCacheType().getCode());
        // 验证码类型
        properties.setProperty(CaptchaPropertiesConst.CAPTCHA_TYPE, config.getServiceType().getCode());
        // 滑块底图路径
        properties.setProperty(CaptchaPropertiesConst.ORIGINAL_PATH_SILIDER, config.getJigsaw());
        // 点选底图路径
        properties.setProperty(CaptchaPropertiesConst.ORIGINAL_PATH_PIC_CLICK, config.getPicClick());
        // 右下角水印文字(我的水印)
        properties.setProperty(CaptchaPropertiesConst.CAPTCHA_WATER_MARK, config.getWaterMark());
        // 点选文字验证码的文字字体(宋体)
        properties.setProperty(CaptchaPropertiesConst.CAPTCHA_FONT_TYPE, config.getFontType());
        properties.setProperty(CaptchaPropertiesConst.CAPTCHA_FONT_STYLE, String.valueOf(config.getFontStyle()));
        properties.setProperty(CaptchaPropertiesConst.CAPTCHA_FONT_SIZE, String.valueOf(config.getFontSize()));
        // 滑块干扰项(0/1/2)
        properties.setProperty(CaptchaPropertiesConst.CAPTCHA_INTERFERENCE_OPTIONS, config.getInterferenceOptions());
        // 滑块误差偏移量
        properties.setProperty(CaptchaPropertiesConst.CAPTCHA_SLIP_OFFSET, config.getSlipOffset());
        // aes加密开关
        properties.setProperty(CaptchaPropertiesConst.CAPTCHA_AES_STATUS, String.valueOf(config.getAesStatus()));
        // 右下角水印字体(宋体)
        properties.setProperty(CaptchaPropertiesConst.CAPTCHA_WATER_FONT, config.getWaterFont());
        // local缓存的阈值
        properties.setProperty(CaptchaPropertiesConst.CAPTCHA_CACAHE_MAX_NUMBER, config.getCacheNumber());
        // 定时清理过期local缓存，秒
        properties.setProperty(CaptchaPropertiesConst.CAPTCHA_TIMING_CLEAR_SECOND, config.getTimingClear());
        // 接口限流开关 0禁用 1启用
        properties.setProperty(CaptchaPropertiesConst.REQ_FREQUENCY_LIMIT_ENABLE, String.valueOf(config.isReqFrequencyLimitEnable()));
        // get 接口 一分钟请求次数限制
        properties.setProperty(CaptchaPropertiesConst.REQ_GET_MINUTE_LIMIT, String.valueOf(config.getReqGetMinuteLimit()));
        // 验证失败后，get接口锁定时间
        properties.setProperty(CaptchaPropertiesConst.REQ_GET_LOCK_LIMIT, String.valueOf(config.getReqGetLockLimit()));
        properties.setProperty(CaptchaPropertiesConst.REQ_GET_LOCK_SECONDS, String.valueOf(config.getReqGetLockSeconds()));
        // verify 接口 一分钟请求次数限制
        properties.setProperty(CaptchaPropertiesConst.REQ_VALIDATE_MINUTE_LIMIT, String.valueOf(config.getReqVerifyMinuteLimit()));
        // check接口 一分钟请求次数限制
        properties.setProperty(CaptchaPropertiesConst.REQ_CHECK_MINUTE_LIMIT, String.valueOf(config.getReqCheckMinuteLimit()));
        // 点选文字个数
        properties.setProperty(CaptchaPropertiesConst.CAPTCHA_WORD_COUNT, String.valueOf(config.getClickWordCount()));
        // 旋转底图路径
        properties.setProperty(CaptchaPropertiesConst.ORIGINAL_PATH_ROTATE, config.getRotate());

        return CaptchaServiceFactory.getCaptchaService(properties);
    }




}
