package com.peach.captcha.service.impl;

import com.peach.captcha.CaptchaProperties;
import com.peach.captcha.constant.CaptchPropertiesConst;
import com.peach.captcha.constant.CaptchaConst;
import com.peach.captcha.constant.CaptchaEnum;
import com.peach.captcha.limit.DefaultFrequencyLimitHandler;
import com.peach.captcha.service.CaptchaCacheService;
import com.peach.captcha.service.CaptchaService;
import com.peach.captcha.factory.CaptchaServiceFactory;
import com.peach.captcha.limit.FrequencyLimitHandler;
import com.peach.captcha.model.CaptchaVO;
import com.peach.captcha.util.AesUtil;
import com.peach.captcha.util.CaptchaImageUtil;
import com.peach.captcha.util.MemoryCacheUtil;
import com.peach.common.util.Md5Util;
import com.peach.common.response.Response;
import com.peach.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.util.Properties;


/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 15:00
 */
@Slf4j
public abstract class AbstractCacheService implements CaptchaService {

    /**
     * check校验坐标
     */
    protected static String REDIS_CAPTCHA_KEY = "RUNNING:CAPTCHA:%s";

    /**
     * 后台二次校验坐标
     */
    protected static String REDIS_SECOND_CAPTCHA_KEY = "RUNNING:CAPTCHA:second-%s";


    private static FrequencyLimitHandler frequencyLimitHandler;

    protected static final String IMAGE_TYPE_PNG = "png";

    protected static int HAN_ZI_SIZE = 25;

    protected static int HAN_ZI_SIZE_HALF = HAN_ZI_SIZE / 2;

    protected static Long EXPIRE_SIN_SECONDS = 2 * 60L;

    protected static Long EXPIRE_SIN_THREE = 3 * 60L;

    protected static String waterMark = "PEACHSOFT";

    protected static String waterMarkFontStr = "WenQuanZhengHei.ttf";

    protected Font waterMarkFont;

    protected static String slipOffset = "5";

    protected static Boolean captchaAesStatus = true;

    protected static String clickWordFontStr = "WenQuanZhengHei.ttf";

    protected static String cacheType = "MEMORY";

    protected static int captchaInterferenceOptions = 0;

    protected static String one = "1";

    protected static String zero = "0";

    protected static String ttf = ".ttf";

    protected static String ttc = ".ttc";

    @Override
    public void init(Properties config) {

        // 如果开启了初始化底图则开始初始化底图
        CaptchaImageUtil.initCaptchaImage(
                config.getProperty(CaptchPropertiesConst.ORIGINAL_PATH_SILIDER),
                config.getProperty(CaptchPropertiesConst.ORIGINAL_PATH_PIC_CLICK),
                config.getProperty(CaptchPropertiesConst.ORIGINAL_PATH_ROTATE));

        waterMark = config.getProperty(CaptchPropertiesConst.CAPTCHA_WATER_MARK, "PEACHSOFT");
        slipOffset = config.getProperty(CaptchPropertiesConst.CAPTCHA_SLIP_OFFSET, "5");
        waterMarkFontStr = config.getProperty(CaptchPropertiesConst.CAPTCHA_WATER_FONT, "WenQuanZhengHei.ttf");
        captchaAesStatus = Boolean.parseBoolean(config.getProperty(CaptchPropertiesConst.CAPTCHA_AES_STATUS, "true"));
        clickWordFontStr = config.getProperty(CaptchPropertiesConst.CAPTCHA_FONT_TYPE, "WenQuanZhengHei.ttf");
        cacheType = config.getProperty(CaptchPropertiesConst.CAPTCHA_CACHETYPE, "MEMORY");
        captchaInterferenceOptions = Integer.parseInt(config.getProperty(CaptchPropertiesConst.CAPTCHA_INTERFERENCE_OPTIONS, "0"));

        // 部署在linux中，如果没有安装中文字段，水印和点选文字，中文无法显示，
        // 通过加载resources下的font字体解决，无需在linux中安装字体
        loadWaterMarkFont();
        if ("MEMORY".equals(cacheType)) {
            MemoryCacheUtil.init(Integer.parseInt(config.getProperty(CaptchPropertiesConst.CAPTCHA_CACAHE_MAX_NUMBER, "1000")),
                    Long.parseLong(config.getProperty(CaptchPropertiesConst.CAPTCHA_TIMING_CLEAR_SECOND, "180")));
        }
        if (one.equals(config.getProperty(CaptchPropertiesConst.REQ_FREQUENCY_LIMIT_ENABLE, zero)) && frequencyLimitHandler == null){
            synchronized (this){
                if (frequencyLimitHandler == null){
                    frequencyLimitHandler = new DefaultFrequencyLimitHandler(config, getCacheService(cacheType));
                }
            }
        }
    }

    /**
     * 加载resources下的font字体，add by Devli
     * 部署在linux中，如果没有安装中文字段，水印和点选文字，中文无法显示，
     * 通过加载resources下的font字体解决，无需在linux中安装字体
     */
    private void loadWaterMarkFont() {
        try {
            if (waterMarkFontStr.toLowerCase().endsWith(ttf) || waterMarkFontStr.toLowerCase().endsWith(ttc)
                    || waterMarkFontStr.toLowerCase().endsWith(".otf")) {
                this.waterMarkFont = Font.createFont(Font.TRUETYPE_FONT,
                                getClass().getResourceAsStream("/fonts/" + waterMarkFontStr))
                        .deriveFont(Font.BOLD, HAN_ZI_SIZE / 2);
            } else {
                this.waterMarkFont = new Font(waterMarkFontStr, Font.BOLD, HAN_ZI_SIZE / 2);
            }

        } catch (Exception e) {
            log.error("load font error:{}", e);
        }
    }

    @Override
    public Response get(CaptchaVO captchaVO) {
        if (frequencyLimitHandler != null){
            captchaVO.setClientUid(getValidateClientId(captchaVO));
            return frequencyLimitHandler.validateGet(captchaVO);
        }
        return null;
    }

    @Override
    public Response check(CaptchaVO captchaVO) {
        if (frequencyLimitHandler != null){
            captchaVO.setClientUid(getValidateClientId(captchaVO));
            return frequencyLimitHandler.validateGet(captchaVO);
        }
        return null;
    }

    @Override
    public Response verification(CaptchaVO captchaVO) {
        if (captchaVO == null || StringUtil.isEmpty(captchaVO.getCaptchaVerification())){
            log.error("captchaVO or captchaVerification is empty");
            return Response.paramError();
        }
        if (frequencyLimitHandler != null){
            captchaVO.setClientUid(getValidateClientId(captchaVO));
            return frequencyLimitHandler.validateGet(captchaVO);
        }
        return null;
    }

    protected CaptchaCacheService getCacheService(String type) {
        return CaptchaServiceFactory.getCaptchaCacheService(type);
    }

    protected String getValidateClientId(CaptchaVO captchaVO){
        if (StringUtil.isBlank(captchaVO.getBrowserInfo())) {
            return Md5Util.md5(captchaVO.getBrowserInfo());
        }
        if (StringUtil.isNotBlank(captchaVO.getClientUid())){
            return captchaVO.getClientUid();
        }
        return null;
    }

    protected boolean validatedReq(Response resp) {
        return resp == null || resp.isSuccess();
    }

    /**
     * 解密前端坐标aes加密
     *
     * @param point
     * @return
     * @throws Exception
     */
    protected  String decrypt(String point, String key) throws Exception {
        return AesUtil.aesDecrypt(point, key);
    }

    protected void afterValidateFail(CaptchaVO data) {
        if (frequencyLimitHandler != null) {
            String fails = String.format(CaptchaConst.REQ_GET_LOCK_FAIL, "FAIL", data.getClientUid());
            CaptchaCacheService cs = getCacheService(cacheType);
            if (!cs.exists(fails)) {
                cs.set(fails, "1", 60);
            }
            cs.increment(fails, 1);
        }
    }


}

