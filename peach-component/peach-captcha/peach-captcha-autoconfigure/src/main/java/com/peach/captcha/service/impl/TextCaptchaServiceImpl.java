package com.peach.captcha.service.impl;

import com.peach.captcha.model.CaptchaVO;
import com.peach.captcha.util.AesUtil;
import com.peach.captcha.util.CaptchaImageUtil;
import com.peach.captcha.util.RandomUtils;
import com.peach.common.keymanager.RedisKeyBuild;
import com.peach.common.keymanager.RedisKeyManage;
import com.peach.common.response.Response;
import com.peach.common.response.StatusEnum;
import com.peach.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Properties;
import java.util.Random;

/**
 * Text/Arithmetic Captcha Service Implementation
 * 文字/算术验证码服务实现类
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/22 14:00
 */
@Slf4j
public class TextCaptchaServiceImpl extends AbstractCacheService {

    private static final int IMAGE_WIDTH = 160;
    private static final int IMAGE_HEIGHT = 60;

    @Override
    public void init(Properties config) {
        super.init(config);
    }

    @Override
    public Response get(CaptchaVO captchaVO) {
        Response response = super.get(captchaVO);
        if (!validatedReq(response)) {
            return response;
        }

        // 1. Generate captcha image and answer / 生成验证码图片和答案
        Object[] imageAndAnswer = generateImageAndAnswer();
        BufferedImage image = (BufferedImage) imageAndAnswer[0];
        String answer = (String) imageAndAnswer[1];

        // 2. Convert to Base64 / 转换为Base64
        String imageBase64 = CaptchaImageUtil.getImageToBase64Str(image);
        captchaVO.setSlidingOriginalImageBase64(imageBase64);
        captchaVO.setToken(RandomUtils.getUuid());

        // 3. Generate secret key (for decrypting answer submitted by frontend) / 生成密钥（用于解密前端提交的答案）
        String secretKey = AesUtil.getKey();
        if (CAPTCHA_AES_STATUS) {
            captchaVO.setSecretKey(secretKey);
        }

        // 4. Store in Redis (Format: answer#secretKey) / 存入Redis（格式：答案#密钥）
        String codeKey = RedisKeyBuild
                .createRedisKey(RedisKeyManage.RUNNING_CAPTCHA, captchaVO.getToken())
                .getRealKey();
        
        setCaptchaCahche(codeKey, answer + "#" + secretKey);

        return Response.success(captchaVO);
    }

    @Override
    public Response check(CaptchaVO captchaVO) {
        Response response = super.check(captchaVO);
        if (!validatedReq(response)) {
            return response;
        }

        String codeKey = RedisKeyBuild
                .createRedisKey(RedisKeyManage.RUNNING_CAPTCHA, captchaVO.getToken())
                .getRealKey();

        if (!existCaptchaKey(codeKey)) {
            log.error("captcha check not found, key: {}", codeKey);
            return Response.fail(StatusEnum.API_CAPTCHA_INVALID);
        }

        String val = getCaptchaByKey(codeKey);
        deleteCaptchKey(codeKey);

        if (StringUtil.isBlank(val)) {
            log.error("captcha check value empty, key: {}", codeKey);
            return Response.fail(StatusEnum.API_CAPTCHA_INVALID);
        }

        String[] parts = val.split("#");
        if (parts.length != 2) {
            log.error("captcha check value format error, val: {}", val);
            return Response.fail(StatusEnum.API_CAPTCHA_INVALID);
        }
        String rightAnswer = parts[0];
        String secretKey = parts[1];

        try {
            // Decrypt user submitted answer / 解密用户提交的答案
            String pointJson = captchaVO.getAnswer();
            // Compatible with cases where pointJson is used for answer / 兼容pointJson用于传答案的情况
            String userAnswer = AesUtil.aesDecrypt(pointJson, secretKey);
            
            // Compare (ignore case) / 比较（忽略大小写）
            if (!rightAnswer.equalsIgnoreCase(userAnswer)) {
                return Response.fail("validate fail");
            }

            // Secondary verification Token / 二次校验Token
            String value = AesUtil.aesEncrypt(captchaVO.getToken().concat("@").concat(userAnswer), secretKey);
            log.info("captcha secretKey:{}, value:{}", secretKey, value);

            String secondKey = RedisKeyBuild
                    .createRedisKey(RedisKeyManage.RUNNING_CAPTCHA_SECOND, value)
                    .getRealKey();
            setCaptchaCahche(secondKey, captchaVO.getToken());

            captchaVO.setResult(true);
            captchaVO.setCaptchaVerification(value);
            return Response.success(captchaVO);

        } catch (Exception e) {
            log.error("Text captcha check error", e);
            return Response.fail(e.getMessage());
        }
    }

    @Override
    public Response verification(CaptchaVO captchaVO) {
        Response r = super.verification(captchaVO);
        if (!validatedReq(r)) {
            return r;
        }

        try {
            String codeKey = RedisKeyBuild
                    .createRedisKey(RedisKeyManage.RUNNING_CAPTCHA_SECOND, captchaVO.getCaptchaVerification())
                    .getRealKey();
            if (!existCaptchaKey(codeKey)) {
                log.error("captcha verification not found, key: {}", codeKey);
                return Response.fail(StatusEnum.API_CAPTCHA_INVALID);
            }
            // Secondary validation token invalid immediately after use / 二次校验取值后，即刻失效
            deleteCaptchKey(codeKey);
            return Response.success();
        } catch (Exception e) {
            log.error("captcha verification error, key: {}", captchaVO.getCaptchaVerification(), e);
            return Response.fail(e.getMessage());
        }
    }

    /**
     * Generate image and answer / 生成图片和答案
     * @return Object[]{BufferedImage, String}
     */
    private Object[] generateImageAndAnswer() {
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // Background / 背景
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        // Antialiasing / 抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        String displayText = "";
        String answer = "";
        
        // Retry logic for text length / 文本长度重试逻辑
        int maxRetries = 3;
        // Initial font size / 初始字体大小
        int fontSize = 30;
        Font font = null;
        FontMetrics fm = null;
        
        for (int i = 0; i <= maxRetries; i++) {
            // Generate content / 生成内容
            Object[] content = generateContent();
            displayText = (String) content[0];
            answer = (String) content[1];

            // If last retry, use fallback / 如果是最后一次重试，使用兜底方案
            if (i == maxRetries) {
                displayText = "1 + 1 = ?";
                answer = "2";
            }

            // Check if text fits / 检查文本是否合适
            font = this.WARK_MARK_FRONT != null ? this.WARK_MARK_FRONT.deriveFont(Font.BOLD, (float)fontSize) : new Font("Arial", Font.BOLD, fontSize);
            g2d.setFont(font);
            fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(displayText);

            // If text fits with current font or can be scaled down effectively
            // Check against minimum font size (12) / 检查最小字体大小(12)
            // Estimate if it will fit even at size 12 / 估算在12号字体下是否能放下
            // 30px -> width W. 12px -> width approx W * (12/30).
            // We want W_12 < IMAGE_WIDTH - 20
            
            // Simple check: Check width at 12px / 简单检查：检查12px时的宽度
            Font minFont = font.deriveFont(Font.BOLD, 12f);
            g2d.setFont(minFont);
            FontMetrics minFm = g2d.getFontMetrics();
            int minWidth = minFm.stringWidth(displayText);
            
            if (minWidth < IMAGE_WIDTH - 20) {
                // Fits! Restore original font for scaling logic below / 放得下！恢复原字体用于下方的缩放逻辑
                g2d.setFont(font);
                fm = g2d.getFontMetrics();
                break;
            }
            // Doesn't fit, retry / 放不下，重试
        }

        // Draw interference lines / 绘制干扰线
        drawInterference(g2d);

        // Draw text / 绘制文字
        g2d.setColor(Color.BLACK);
        
        // Dynamic font scaling / 动态字体缩放
        int textWidth = fm.stringWidth(displayText);
        
        // Reduce font size if text is too wide / 如果文字太宽则缩小字体
        while (textWidth > IMAGE_WIDTH - 20 && fontSize > 12) {
            fontSize -= 2;
            font = font.deriveFont(Font.BOLD, (float)fontSize);
            g2d.setFont(font);
            fm = g2d.getFontMetrics();
            textWidth = fm.stringWidth(displayText);
        }

        // Center text / 文字居中
        int x = (IMAGE_WIDTH - textWidth) / 2;
        int y = (IMAGE_HEIGHT - fm.getHeight()) / 2 + fm.getAscent();

        g2d.drawString(displayText, x, y);

        // Draw noise / 绘制噪点
        drawNoise(image);

        g2d.dispose();
        return new Object[]{image, answer};
    }

    /**
     * Generate content (Math or Text) / 生成内容（算术或文字）
     * @return Object[]{displayText, answer}
     */
    private Object[] generateContent() {
        boolean isMath = new Random().nextBoolean();
        String displayText;
        String answer;

        if (isMath) {
            // Arithmetic / 算术
            int num1 = RandomUtils.getRandomInt(1, 20);
            int num2 = RandomUtils.getRandomInt(1, 20);
            int operator = RandomUtils.getRandomInt(0, 3); // 0:+, 1:-, 2:*
            
            if (operator == 0) {
                displayText = num1 + " + " + num2 + " = ?";
                answer = String.valueOf(num1 + num2);
            } else if (operator == 1) {
                if (num1 < num2) { int temp = num1; num1 = num2; num2 = temp; }
                displayText = num1 + " - " + num2 + " = ?";
                answer = String.valueOf(num1 - num2);
            } else {
                // Multiplication / 乘法
                num1 = RandomUtils.getRandomInt(1, 10);
                num2 = RandomUtils.getRandomInt(1, 10);
                displayText = num1 + " * " + num2 + " = ?";
                answer = String.valueOf(num1 * num2);
            }
        } else {
            // Character / 字符
            answer = RandomUtils.getRandomString(5);
            displayText = answer;
        }
        return new Object[]{displayText, answer};
    }

    /**
     * Draw interference lines / 绘制干扰线
     * @param g Graphics2D
     */
    private void drawInterference(Graphics2D g) {
        g.setColor(Color.LIGHT_GRAY);
        for (int i = 0; i < 5; i++) {
            int x1 = RandomUtils.getRandomInt(0, IMAGE_WIDTH);
            int y1 = RandomUtils.getRandomInt(0, IMAGE_HEIGHT);
            int x2 = RandomUtils.getRandomInt(0, IMAGE_WIDTH);
            int y2 = RandomUtils.getRandomInt(0, IMAGE_HEIGHT);
            g.drawLine(x1, y1, x2, y2);
        }
    }

    /**
     * Draw noise pixels / 绘制噪点
     * @param image BufferedImage
     */
    private void drawNoise(BufferedImage image) {
        Random random = new Random();
        for (int i = 0; i < 50; i++) {
            int x = random.nextInt(IMAGE_WIDTH);
            int y = random.nextInt(IMAGE_HEIGHT);
            image.setRGB(x, y, new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)).getRGB());
        }
    }
}
