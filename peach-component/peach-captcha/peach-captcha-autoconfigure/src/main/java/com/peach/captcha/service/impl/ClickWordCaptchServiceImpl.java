package com.peach.captcha.service.impl;

import com.alibaba.fastjson.JSON;
import com.peach.captcha.constant.CaptchaPropertiesConst;
import com.peach.captcha.model.CaptchaVO;
import com.peach.captcha.model.PointVO;
import com.peach.captcha.util.AesUtil;
import com.peach.captcha.util.CaptchaImageUtil;
import com.peach.captcha.util.JsonUtil;
import com.peach.captcha.util.RandomUtils;
import com.peach.common.keymanager.RedisKeyBuild;
import com.peach.common.keymanager.RedisKeyManage;
import com.peach.common.response.Response;
import com.peach.common.response.StatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * ClickWordCaptch服务实现类。
 * 点选文字验证码服务实现类
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/22 11:36
 */
@Slf4j
public class ClickWordCaptchServiceImpl extends AbstractCacheService {

    /**
     * Click word font / 点选文字字体
     */
    protected Font clickWordFont;

    /**
     * Total number of words / 点选文字字体总个数
     */
    private int wordTotalCount = 4;
    
    /**
     * Whether font color is random / 点选文字字体颜色是否随机
     */
    private boolean fontColorRandom = true;

    /**
     * Initialize configuration / 初始化配置
     * @param config Properties
     */
    @Override
    public void init(Properties config) {
        super.init(config);
        applyClickWordFontType(config);
        try {
            int size = Integer.parseInt(config.getProperty(CaptchaPropertiesConst.CAPTCHA_FONT_SIZE, HAN_ZI_SIZE + ""));

            if (CLICK_WORD_FRONT_STR.toLowerCase().endsWith(".ttf")
                    || CLICK_WORD_FRONT_STR.toLowerCase().endsWith(".ttc")
                    || CLICK_WORD_FRONT_STR.toLowerCase().endsWith(".otf")) {
                this.clickWordFont = Font.createFont(Font.TRUETYPE_FONT,
                                getClass().getResourceAsStream("/fonts/" + CLICK_WORD_FRONT_STR))
                        .deriveFont(Font.BOLD, size);
            } else {
                int style = Integer.parseInt(config.getProperty(CaptchaPropertiesConst.CAPTCHA_FONT_STYLE, Font.BOLD + ""));
                this.clickWordFont = new Font(CLICK_WORD_FRONT_STR, style, size);
            }
        } catch (Exception ex) {
            log.error("load font error:{}", ex.getMessage());
        }
        this.wordTotalCount = Integer.parseInt(config.getProperty(CaptchaPropertiesConst.CAPTCHA_WORD_COUNT, "4"));
    }

    private static void applyClickWordFontType(Properties config) {
        CLICK_WORD_FRONT_STR = config.getProperty(CaptchaPropertiesConst.CAPTCHA_FONT_TYPE, "SourceHanSansCN-Normal.otf");
    }

    /**
     * Get Captcha / 获取验证码
     * @param captchaVO Captcha parameters
     * @return Response
     */
    @Override
    public Response get(CaptchaVO captchaVO) {
        Response response = super.get(captchaVO);
        if (!validatedReq(response)) {
            return response;
        }

        // 1. Get background image / 获取底图
        BufferedImage backgroundImage = CaptchaImageUtil.getPicClickImage();
        if (backgroundImage == null) {
            log.error("init original click word image error");
            return Response.fail("init original click word image error");
        }

        // 2. Generate captcha data / 生成验证码数据
        CaptchaVO imageData = getImageData(backgroundImage);
        if (imageData == null
                || StringUtils.isBlank(imageData.getPicClickBase64())) {
            return Response.fail("captcha image generate failed");
        }
        return Response.success(imageData);
    }

    /**
     * Check Captcha / 校验验证码
     * @param captchaVO Captcha parameters (including encrypted coordinates) / 验证码参数（包含加密的坐标信息）
     * @return Response
     */
    @Override
    public Response check(CaptchaVO captchaVO) {
        Response response = super.check(captchaVO);
        if (!validatedReq(response)){
            return response;
        }
        
        // 1. Get Key from Redis / 获取 Redis 中的 Key
        String codeKey = RedisKeyBuild
                .createRedisKey(RedisKeyManage.RUNNING_CAPTCHA, captchaVO.getToken())
                .getRealKey();
        if (!existCaptchaKey(codeKey)){
            log.error("captcha check not found, key: {}", codeKey);
            return Response.fail(StatusEnum.API_CAPTCHA_INVALID);
        }

        // 2. Get cached correct points / 获取缓存的正确坐标点
        String s = getCaptchaByKey(codeKey);
        // Delete immediately after retrieval to prevent replay / 取出后立即删除，防止重放
        deleteCaptchKey(codeKey);

        List<PointVO> cachePoints = null;
        List<PointVO> userPoints = new ArrayList<>();
        try {
            // Parse cached points (including independent secret key for each point) / 解析缓存中的点（包含每个点的独立密钥）
            cachePoints = JsonUtil.parseArray(s);

            // 3. Parse coordinate data submitted by frontend / 解析前端提交的坐标数据
            // Frontend pointJson should be a JSON array of encrypted strings / 前端传来的 pointJson 应该是一个包含多个加密字符串的 JSON 数组
            // e.g. ["Encrypted Point 1", "Encrypted Point 2"]
            List<String> encryptedPointList = JSON.parseArray(captchaVO.getAnswer(), String.class);

            if (cachePoints.size() != encryptedPointList.size()) {
                 return Response.fail("validate fail: point count mismatch");
            }

            String secretKey = cachePoints.get(0).secretKey();
            // 4. Decrypt and verify one by one / 逐个解密验证
            for (int i = 0; i < cachePoints.size(); i++) {
                // Decrypt using corresponding key / 使用对应的密钥解密
                String decryptedPointJson = AesUtil.aesDecrypt(encryptedPointList.get(i), secretKey);
                PointVO point = JsonUtil.parseObject(decryptedPointJson, PointVO.class);
                userPoints.add(point);
            }

        } catch (Exception e) {
            log.error("verify parse error", e);
            return Response.fail(e.getMessage());
        }

        if (cachePoints == null || userPoints == null || cachePoints.size() != userPoints.size()) {
            return Response.fail("validate fail");
        }

        // 5. Check coordinate deviation for each point / 校验每个点的坐标偏差
        for (int i = 0; i < cachePoints.size(); i++) {
            PointVO target = cachePoints.get(i);
            PointVO source = userPoints.get(i);
            // Allow error 25px / 允许误差 25px
            if (Math.abs(target.x() - source.x()) > 25 || Math.abs(target.y() - source.y()) > 25) {
                return Response.fail("validate fail");
            }
        }

        // 6. Verification passed, generate secondary verification token / 校验通过，生成二次校验 token
        // Encrypt result using first point's key / 使用第一个点的密钥加密结果
        String secretKey = cachePoints.get(0).secretKey();
        String value;
        try {
            value = AesUtil.aesEncrypt(captchaVO.getToken().concat("@").concat(JsonUtil.toJsonString(userPoints)), secretKey);
        } catch (Exception e) {
            log.error("AES encrypt error", e);
            return Response.fail(e.getMessage());
        }
        
        String secondKey = RedisKeyBuild
                .createRedisKey(RedisKeyManage.RUNNING_CAPTCHA_SECOND, value)
                .getRealKey();
        setCaptchaCahche(secondKey, captchaVO.getToken());
        
        captchaVO.setResult(true);
        captchaVO.setCaptchaVerification(value);
        
        return Response.success();
    }

    @Override
    public Response verification(CaptchaVO captchaVO) {
        Response response = super.verification(captchaVO);
        if (!validatedReq(response)){
            return response;
        }
        
        String codeKey = RedisKeyBuild
                .createRedisKey(RedisKeyManage.RUNNING_CAPTCHA_SECOND, captchaVO.getCaptchaVerification())
                .getRealKey();

        if (!existCaptchaKey(codeKey)){
            log.error("captcha verification not found, key: {}", codeKey);
            return Response.fail(StatusEnum.API_CAPTCHA_INVALID);
        }
        deleteCaptchKey(codeKey);
        return Response.success();
    }
    
    /**
     * Generate non-overlapping random coordinates / 生成不重叠的随机坐标
     */
    private PointVO generateRandomPoint(int width, int height, List<PointVO> existPoints) {
        // Simple retry mechanism / 简单的重试机制
        for (int i = 0; i < 100; i++) {
            int x = RandomUtils.getRandomInt(20, width - 20 - HAN_ZI_SIZE);
            int y = RandomUtils.getRandomInt(20 + HAN_ZI_SIZE, height - 20);
            
            boolean overlap = false;
            for (PointVO p : existPoints) {
                // Distance check to prevent overlap / 距离判断，防止重叠
                if (Math.sqrt(Math.pow((double) x - p.x(), 2) + Math.pow((double) y - p.y(), 2)) < HAN_ZI_SIZE * 1.5) {
                    overlap = true;
                    break;
                }
            }
            if (!overlap) {
                return new PointVO(null, x, y);
            }
        }
        // If fail to find, return random one (low probability) / 如果实在找不到，就随便返回一个，概率很小
        return new PointVO(null, RandomUtils.getRandomInt(20, width - 20), RandomUtils.getRandomInt(20, height - 20));
    }

    public int getWordTotalCount() {
        return wordTotalCount;
    }

    public void setWordTotalCount(int wordTotalCount) {
        this.wordTotalCount = wordTotalCount;
    }

    public boolean isFontColorRandom() {
        return fontColorRandom;
    }

    public void setFontColorRandom(boolean fontColorRandom) {
        this.fontColorRandom = fontColorRandom;
    }

    /**
     * Generate captcha image and related data / 生成验证码图片及相关数据
     * @param backgroundImage Background image / 底图
     * @return CaptchaVO
     */
    private CaptchaVO getImageData(BufferedImage backgroundImage) {
        int width = backgroundImage.getWidth();
        int height = backgroundImage.getHeight();

        // Create ARGB canvas / 创建 ARGB 画布
        BufferedImage originalImage =
                new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D graphics = originalImage.createGraphics();

        // Initialize background / 初始化背景
        graphics.setComposite(AlphaComposite.SrcOver);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        graphics.drawImage(backgroundImage, 0, 0, null);
        
        // Draw watermark / 绘制水印
        if (this.waterMarkFont != null) {
             graphics.setFont(waterMarkFont);
             graphics.setColor(Color.white);
             graphics.drawString(WATER_MARK, width - getEnOrChLength(WATER_MARK), height - (HAN_ZI_SIZE / 2) + 7);
        }

        // Generate random Chinese characters / 生成随机汉字
        int wordCount = 5;
        int checkCount = 3;

        List<String> words = new ArrayList<>();
        while (words.size() < wordCount) {
            String s = RandomUtils.getRandomHan();
            if (!words.contains(s)) {
                words.add(s);
            }
        }

        // Randomly select indices to click / 随机选择需要点击的汉字索引
        List<Integer> checkIndices = new ArrayList<>();
        List<String> checkWords = new ArrayList<>();
        while (checkIndices.size() < checkCount) {
            int index = RandomUtils.getRandomInt(0, wordCount);
            if (!checkIndices.contains(index)) {
                checkIndices.add(index);
                checkWords.add(words.get(index));
            }
        }

        List<PointVO> pointList = new ArrayList<>();
        List<PointVO> checkPointList = new ArrayList<>();

        Font font = this.clickWordFont;
        graphics.setFont(font);

        // Draw all characters on image / 绘制所有汉字到图片上
        for (int i = 0; i < wordCount; i++) {
            String word = words.get(i);
            PointVO point = generateRandomPoint(width, height, pointList);
            pointList.add(point);

            graphics.setColor(new Color(
                    RandomUtils.getRandomInt(0, 255),
                    RandomUtils.getRandomInt(0, 255),
                    RandomUtils.getRandomInt(0, 255)));

            AffineTransform at = new AffineTransform();
            at.rotate(Math.toRadians(RandomUtils.getRandomInt(-45, 45)));
            graphics.setFont(font.deriveFont(at));

            graphics.drawString(word, point.x(), point.y());
        }

        graphics.setFont(font);
        FontMetrics fm = graphics.getFontMetrics();
        int textHeight = fm.getAscent();

        // Calculate center coordinates for clicked characters and assign independent key / 计算需要点击的汉字的中心坐标，并为每个点分配独立的密钥
        String secretKey = AesUtil.getKey();
        if (CAPTCHA_AES_STATUS){
            secretKey = AesUtil.getKey();
        }
        for (Integer index : checkIndices) {
            PointVO p = pointList.get(index);
            String word = words.get(index);
            int textWidth = fm.stringWidth(word);

            int centerX = p.x() + textWidth / 2;
            int centerY = p.y() - textHeight / 2;
            checkPointList.add(new PointVO(secretKey, centerX, centerY));
        }

        graphics.dispose();

        // Generate hint image (Force ARGB) / 生成提示图（强制 ARGB）
        BufferedImage tipImage = new BufferedImage(240, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D tipG = tipImage.createGraphics();

        tipG.setComposite(AlphaComposite.SrcOver);
        tipG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        tipG.setColor(Color.WHITE);
        tipG.fillRect(0, 0, 240, 40);

        tipG.setColor(Color.BLACK);
        tipG.setFont(new Font(CLICK_WORD_FRONT_STR, Font.BOLD, 20));
        tipG.drawString("请依次点击：", 10, 25);

        int xOffset = 130;
        for (String word : checkWords) {
            tipG.setColor(new Color(
                    RandomUtils.getRandomInt(0, 150),
                    RandomUtils.getRandomInt(0, 150),
                    RandomUtils.getRandomInt(0, 150)));
            tipG.drawString(word, xOffset, 25);
            xOffset += 25;
        }

        tipG.dispose();

        // Base64 conversion / Base64 转换
        String originalImageBase64 = CaptchaImageUtil.getImageToBase64Str(originalImage);
        String tipImageBase64 = CaptchaImageUtil.getImageToBase64Str(tipImage);

        if (StringUtils.isBlank(originalImageBase64) || StringUtils.isBlank(tipImageBase64)) {
            log.error("image base64 empty, original={}, tip={}",
                    originalImageBase64.length(), tipImageBase64.length());
            return null;
        }

        CaptchaVO vo = new CaptchaVO();
        vo.setPicClickBase64(originalImageBase64);
        vo.setPicClickpromptBase64(tipImageBase64);
        vo.setToken(RandomUtils.getUuid());
        vo.setSecretKey(secretKey);

        String codeKey = RedisKeyBuild
                .createRedisKey(RedisKeyManage.RUNNING_CAPTCHA, vo.getToken())
                .getRealKey();

        // Store correct coordinates in Redis / 将正确坐标存入 Redis
        setCaptchaCahche(codeKey, JsonUtil.toJsonString(checkPointList));
        return vo;
    }

}
