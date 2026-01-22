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
 * 文本/运算验证码服务实现类
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

        // 1. 生成验证码图片和答案
        Object[] imageAndAnswer = generateImageAndAnswer();
        BufferedImage image = (BufferedImage) imageAndAnswer[0];
        String answer = (String) imageAndAnswer[1];

        // 2. 转换 Base64
        String imageBase64 = CaptchaImageUtil.getImageToBase64Str(image);
        captchaVO.setSlidingOriginalImageBase64(imageBase64);
        captchaVO.setToken(RandomUtils.getUuid());

        // 3. 生成密钥 (用于解密前端提交的答案)
        String secretKey = AesUtil.getKey();
        if (CAPTCHA_AES_STATUS) {
            captchaVO.setSecretKey(secretKey);
        }

        // 4. 存入 Redis (存储 答案 + 密钥)
        String codeKey = RedisKeyBuild
                .createRedisKey(RedisKeyManage.RUNNING_CAPTCHA, captchaVO.getToken())
                .getRealKey();
        
        // 格式: 答案#密钥
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
            return Response.fail(StatusEnum.API_CAPTCHA_INVALID);
        }

        String val = getCaptchaByKey(codeKey);
        deleteCaptchKey(codeKey);

        if (StringUtil.isBlank(val)) {
            return Response.fail(StatusEnum.API_CAPTCHA_INVALID);
        }

        String[] parts = val.split("#");
        if (parts.length != 2) {
            return Response.fail(StatusEnum.API_CAPTCHA_INVALID);
        }
        String rightAnswer = parts[0];
        String secretKey = parts[1];

        try {
            // 解密用户提交的答案
            String userAnswer = AesUtil.aesDecrypt(captchaVO.getAnswer(), secretKey);
            
            // 比对 (忽略大小写)
            if (!rightAnswer.equalsIgnoreCase(userAnswer)) {
                return Response.fail("validate fail");
            }

            // 二次校验 Token
            String value = AesUtil.aesEncrypt(captchaVO.getToken().concat("@").concat(userAnswer), secretKey);
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

    private Object[] generateImageAndAnswer() {
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // 背景
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        // 抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 随机决定是算术还是字符
        boolean isMath = new Random().nextBoolean();
        String displayText;
        String answer;

        if (isMath) {
            // 算术
            int num1 = RandomUtils.getRandomInt(1, 20);
            int num2 = RandomUtils.getRandomInt(1, 20);
            int operator = RandomUtils.getRandomInt(0, 3); // 0:+, 1:-, 2:*
            
            if (operator == 0) {
                displayText = num1 + " + " + num2 + " = ?";
                answer = String.valueOf(num1 + num2);
            } else if (operator == 1) {
                if (num1 < num2) { int temp = num1; num1 = num2; num2 = temp; } // 保证非负
                displayText = num1 + " - " + num2 + " = ?";
                answer = String.valueOf(num1 - num2);
            } else {
                // 乘法控制小一点
                num1 = RandomUtils.getRandomInt(1, 10);
                num2 = RandomUtils.getRandomInt(1, 10);
                displayText = num1 + " * " + num2 + " = ?";
                answer = String.valueOf(num1 * num2);
            }
        } else {
            // 字符
            answer = RandomUtils.getRandomString(5);
            displayText = answer;
        }

        // 绘制干扰线
        drawInterference(g2d);

        // 绘制文字
        g2d.setColor(Color.BLACK);
        // 使用加载的字体或默认字体
        Font font = this.WARK_MARK_FRONT != null ? this.WARK_MARK_FRONT.deriveFont(Font.BOLD, 30f) : new Font("Arial", Font.BOLD, 30);
        g2d.setFont(font);

        // 简单的居中计算
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(displayText);
        int x = (IMAGE_WIDTH - textWidth) / 2;
        int y = (IMAGE_HEIGHT - fm.getHeight()) / 2 + fm.getAscent();

        // 稍微扭曲/旋转一下文字? 简单起见直接画
        g2d.drawString(displayText, x, y);

        // 绘制噪点
        drawNoise(image);

        g2d.dispose();
        return new Object[]{image, answer};
    }

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

    private void drawNoise(BufferedImage image) {
        Random random = new Random();
        for (int i = 0; i < 50; i++) {
            int x = random.nextInt(IMAGE_WIDTH);
            int y = random.nextInt(IMAGE_HEIGHT);
            image.setRGB(x, y, new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)).getRGB());
        }
    }
}
