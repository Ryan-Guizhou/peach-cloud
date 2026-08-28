package com.peach.captcha.service.impl;

import com.peach.captcha.constant.CaptchaPropertiesConst;
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
import com.peach.common.keymanager.RedisKeyBuild;
import com.peach.common.keymanager.RedisKeyManage;
import com.peach.common.util.Md5Util;
import com.peach.common.response.Response;
import com.peach.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.awt.Font;
import java.nio.charset.StandardCharsets;
import java.util.Properties;


/**
 * 抽象缓存服务类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 15:00
 */
@Slf4j
public abstract class AbstractCacheService implements CaptchaService {

    private static final String DEFAULT_FONT_FILE = "WenQuanZhengHei.ttf";

    private static final String MEMORY_CACHE_TYPE = "MEMORY";

    /**
     * 频率限制处理器 / Frequency limiting processor
     */
    private static FrequencyLimitHandler frequencyLimitHandler;

    /**
     * 默认图片格式 / Default image format
     */
    protected static final String IMAGE_TYPE_PNG = "png";

    /**
     * 汉字大小 / Chinese size
     */
    protected static int HAN_ZI_SIZE = 25;

    /**
     * 汉字大小一半 / Chinese size half
     */
    protected static int HAN_ZI_SIZE_HALF = HAN_ZI_SIZE / 2;

    /**
     * 默认图片过期时间 / Default image expiration time
     */
    protected static Long EXPIRE_SIN_SECONDS = 2 * 60L;

    /**
     * 默认图片过期时间 / Default image expiration time
     */
    protected static Long EXPIRE_SIN_THREE = 3 * 60L;

    /**
     * 水印文字(我的水印) / Watermark text (my watermark)
     */
    protected static String WATER_MARK = "PEACHSOFT";

    /**
     * 水印字体 / Watermark font
     */
    protected static String WATER_MARK_STR = DEFAULT_FONT_FILE;

    /**
     * 水印字体 / Watermark font
     */
    protected Font waterMarkFont;

    /**
     * 滑动误差偏移量 / Slide error offset
     */
    protected static String SLIP_OFFSET = "5";

    /**
     * aes加密开关 / AES encryption switch
     */
    protected static boolean CAPTCHA_AES_STATUS = true;

    /**
     * 点选文字验证码的文字字体(宋体) / Point selection text verification code font (songti)
     */
    protected static String CLICK_WORD_FRONT_STR = DEFAULT_FONT_FILE;

    /**
     * 缓存MEMORY/REDIS / Cache MEMORY/REDIS
     */
    protected static String CACHE_TYPE = MEMORY_CACHE_TYPE;

    /**
     * 滑块干扰项(0/1/2) / Slide interference items (0/1/2)
     */
    protected static int INTERFERENCE_OPTIONS = 0;

    protected static String ONE = "1";

    protected static String ZERO = "0";

    protected static String TTF = ".ttf";

    protected static String TTC = ".ttc";

    @Override
    public void init(Properties config) {

        // 如果开启了初始化底图则开始初始化底图
        CaptchaImageUtil.initCaptchaImage(
                config.getProperty(CaptchaPropertiesConst.ORIGINAL_PATH_SILIDER),
                config.getProperty(CaptchaPropertiesConst.ORIGINAL_PATH_PIC_CLICK),
                config.getProperty(CaptchaPropertiesConst.ORIGINAL_PATH_ROTATE));

        applyStaticConfig(config);

        // 部署在linux中，如果没有安装中文字段，水印和点选文字，中文无法显示，
        // 通过加载resources下的font字体解决，无需在linux中安装字体
        loadWaterMarkFont();
        if (MEMORY_CACHE_TYPE.equals(CACHE_TYPE)) {
            MemoryCacheUtil.init(Integer.parseInt(config.getProperty(CaptchaPropertiesConst.CAPTCHA_CACAHE_MAX_NUMBER, "1000")),
                    Long.parseLong(config.getProperty(CaptchaPropertiesConst.CAPTCHA_TIMING_CLEAR_SECOND, "180")));
        }
        if (ONE.equals(config.getProperty(CaptchaPropertiesConst.REQ_FREQUENCY_LIMIT_ENABLE, ZERO))) {
            initFrequencyLimitHandler(config);
        }
    }

    private static void initFrequencyLimitHandler(Properties config) {
        if (frequencyLimitHandler != null) {
            return;
        }
        synchronized (AbstractCacheService.class) {
            if (frequencyLimitHandler == null) {
                frequencyLimitHandler = new DefaultFrequencyLimitHandler(config,
                        CaptchaServiceFactory.getCaptchaCacheService(CACHE_TYPE));
            }
        }
    }

    private static void applyStaticConfig(Properties config) {
        WATER_MARK = config.getProperty(CaptchaPropertiesConst.CAPTCHA_WATER_MARK, "PEACHSOFT");
        SLIP_OFFSET = config.getProperty(CaptchaPropertiesConst.CAPTCHA_SLIP_OFFSET, "5");
        WATER_MARK_STR = config.getProperty(CaptchaPropertiesConst.CAPTCHA_WATER_FONT, DEFAULT_FONT_FILE);
        CAPTCHA_AES_STATUS = Boolean.parseBoolean(config.getProperty(CaptchaPropertiesConst.CAPTCHA_AES_STATUS, "true"));
        CLICK_WORD_FRONT_STR = config.getProperty(CaptchaPropertiesConst.CAPTCHA_FONT_TYPE, DEFAULT_FONT_FILE);
        CACHE_TYPE = config.getProperty(CaptchaPropertiesConst.CAPTCHA_CACHETYPE, MEMORY_CACHE_TYPE);
        INTERFERENCE_OPTIONS = Integer.parseInt(config.getProperty(CaptchaPropertiesConst.CAPTCHA_INTERFERENCE_OPTIONS, "0"));
    }

    /**
     * 加载resources下的font字体，add by Devli
     * 部署在linux中，如果没有安装中文字段，水印和点选文字，中文无法显示，
     * 通过加载resources下的font字体解决，无需在linux中安装字体
     */
    private void loadWaterMarkFont() {
        try {
            if (WATER_MARK_STR.toLowerCase().endsWith(TTF) || WATER_MARK_STR.toLowerCase().endsWith(TTC)
                    || WATER_MARK_STR.toLowerCase().endsWith(".otf")) {
                this.waterMarkFont = Font.createFont(Font.TRUETYPE_FONT,
                                getClass().getResourceAsStream("/fonts/" + WATER_MARK_STR))
                        .deriveFont(Font.BOLD, HAN_ZI_SIZE / 2f);
            } else {
                this.waterMarkFont = new Font(WATER_MARK_STR, Font.BOLD, HAN_ZI_SIZE / 2);
            }

        } catch (Exception e) {
            log.error("load font error:{}", e);
        }
    }

    @Override
    public Response check(CaptchaVO captchaVO) {
        return invokeFrequencyLimit(captchaVO, true);
    }

    private Response invokeFrequencyLimit(CaptchaVO captchaVO, boolean check) {
        if (frequencyLimitHandler == null) {
            return null;
        }
        captchaVO.setClientUid(getValidateClientId(captchaVO));
        return check ? frequencyLimitHandler.validateCheck(captchaVO) : frequencyLimitHandler.validateGet(captchaVO);
    }

    @Override
    public Response get(CaptchaVO captchaVO) {
        return invokeFrequencyLimit(captchaVO, false);
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

    /**
     * 获取缓存服务 / Get cache service
     * @param type 缓存类型 / Cache type
     * @return 缓存服务 / Cache service
     */
    protected CaptchaCacheService getCacheService(String type) {
        return CaptchaServiceFactory.getCaptchaCacheService(type);
    }

    protected String getValidateClientId(CaptchaVO captchaVO){
        if (StringUtil.isBlank(captchaVO.getBrowserInfo())) {
            if (StringUtil.isNotBlank(captchaVO.getClientUid())){
                return captchaVO.getClientUid();
            }
            return null;
        }
        return Md5Util.sha256Hex(captchaVO.getBrowserInfo());
    }

    /**
     * 验证请求结果 / Verify request result
     * @param resp 响应结果 / Response result
     * @return true:验证成功 / True:Verification successful
     */
    protected boolean validatedReq(Response resp) {
        return resp == null || resp.isSuccess();
    }

    /**
     * 解密前端坐标aes加密 / Decrypt front-end coordinate aes encryption
     * @param point 滑块坐标json / Slide block coordinate json
     * @return 解密后的string / Decrypted string
     * @throws Exception 抛出异常 / Throw an exception
     */
    protected String decrypt(String point, String key) {
        try {
            return AesUtil.aesDecrypt(point, key);
        } catch (Exception ex) {
            throw new IllegalStateException("Captcha coordinate decryption failed", ex);
        }
    }

    /**
     * 验证失败处理 / Verification failure handling
     * @param data 验证失败数据 / Verification failure data
     */
    protected void afterValidateFail(CaptchaVO data) {
        if (frequencyLimitHandler != null) {
            String fails = RedisKeyBuild
                    .createRedisKey(RedisKeyManage.CAPTCHA_REQ_LIMIT,data.getClientUid(),CaptchaEnum.CaptchaOpertionType.FAIL.getCode())
                    .getRealKey();
            CaptchaCacheService cs = getCacheService(CACHE_TYPE);
            if (!cs.exists(fails)) {
                cs.set(fails, "1", 60);
            }
            cs.increment(fails, 1);
        }
    }

    /**
     * 设置验证码缓存 / Set verification code cache
     * @param captchaKey 验证码缓存key / Captcha cache key
     * @param captchaValue 验证码缓存value / Captcha cache value
     */
    protected  void setCaptchaCahche(String captchaKey,String captchaValue) {
        CaptchaCacheService cacheService = getCacheService(CACHE_TYPE);
        cacheService.set(captchaKey,captchaValue, EXPIRE_SIN_THREE);
    }

    /**
     * 验证码缓存是否存在 / Captcha cache exists
     * @param captchaKey 验证码缓存key / Captcha cache key
     * @return true:存在 / True:Exist
     */
    protected boolean existCaptchaKey(String captchaKey) {
        CaptchaCacheService cacheService = getCacheService(CACHE_TYPE);
        return cacheService.exists(captchaKey);
    }

    /**
     * 删除验证码缓存 / Delete verification code cache
     * @param captchaKey 验证码缓存key / Captcha cache key
     */
    protected void deleteCaptchKey(String captchaKey) {
        CaptchaCacheService cacheService = getCacheService(CACHE_TYPE);
        cacheService.delete(captchaKey);
    }

    protected String getCaptchaByKey(String captchaKey){
        CaptchaCacheService cacheService = getCacheService(CACHE_TYPE);
        return cacheService.get(captchaKey);
    }

    protected int getEnOrChLength(String s) {
        int enCount = 0;
        int chCount = 0;
        for (int i = 0; i < s.length(); i++) {
            int length = String.valueOf(s.charAt(i)).getBytes(StandardCharsets.UTF_8).length;
            if (length > 1) {
                chCount++;
            } else {
                enCount++;
            }
        }
        int chOffset = (HAN_ZI_SIZE / 2) * chCount + 5;
        int enOffset = enCount * 8;
        return chOffset + enOffset;
    }

}

