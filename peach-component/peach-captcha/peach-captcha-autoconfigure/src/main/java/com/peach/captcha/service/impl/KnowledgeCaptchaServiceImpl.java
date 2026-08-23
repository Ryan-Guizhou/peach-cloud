package com.peach.captcha.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

/**
 * 知识/常识验证码服务实现类
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/22 14:10
 */
@Slf4j
public class KnowledgeCaptchaServiceImpl extends AbstractCacheService {

    private static final int IMAGE_WIDTH = 200;

    private static final int IMAGE_HEIGHT = 60;
    
    // 简单的内置题库
    private static Map<String, String> QUESTION_BANK = new HashMap<>();

    static {
        ClassLoader classLoader = KnowledgeCaptchaServiceImpl.class.getClassLoader();
        try (InputStream stream = classLoader.getResourceAsStream("defaultImages/knowledge/knowledge.json")) {
            if (stream == null) {
                log.error("knowledge.json not found in classpath");
            } else {
                ObjectMapper OBJECT_MAPPER = new ObjectMapper();
                QUESTION_BANK = OBJECT_MAPPER.readValue(stream, Map.class);
            }
        } catch (Exception e) {
            log.error("Failed to parse knowledge.json", e);
        }
    }

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

        // 1. 随机抽取问题
        Object[] keys = QUESTION_BANK.keySet().toArray();
        String question = (String) keys[RandomUtils.getRandomInt(0, keys.length)];
        String answer = QUESTION_BANK.get(question);

        // 2. 生成图片
        BufferedImage image = generateImage(question);

        // 3. 转换 Base64
        String imageBase64 = CaptchaImageUtil.getImageToBase64Str(image);
        captchaVO.setSlidingOriginalImageBase64(imageBase64);
        captchaVO.setToken(RandomUtils.getUuid());

        // 4. 生成密钥
        String secretKey = AesUtil.getKey();
        if (CAPTCHA_AES_STATUS) {
            captchaVO.setSecretKey(secretKey);
        }

        // 5. 存入 Redis (答案#密钥)
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
            
            // 简单清洗用户输入（去空格）
            if (userAnswer != null) {
                userAnswer = userAnswer.trim();
            }

            // 比对 (包含或相等，提高容错率，例如“是”和“是的”)
            if (!checkAnswer(rightAnswer, userAnswer)) {
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
            log.error("Knowledge captcha check error", e);
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
    
    private boolean checkAnswer(String right, String user) {
        if (user == null) {
            return false;
        }
        // 简单相等
        if (right.equals(user)) {
            return true;
        }
        // 包含关系 (例如 right="北京", user="北京市")
        if (user.contains(right) && user.length() < right.length() + 2){
            return true;
        }
        if (right.contains(user) && right.length() < user.length() + 2) {
            return true;
        }
        return false;
    }

    private BufferedImage generateImage(String text) {
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // 背景
        g2d.setColor(new Color(240, 240, 240));
        g2d.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        // 抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 绘制干扰
        drawInterference(g2d);

        // 绘制文字
        g2d.setColor(Color.DARK_GRAY);
        Font font = this.WARK_MARK_FRONT != null ? this.WARK_MARK_FRONT.deriveFont(Font.BOLD, 22f) : new Font("SansSerif", Font.BOLD, 22);
        g2d.setFont(font);

        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int x = (IMAGE_WIDTH - textWidth) / 2;
        int y = (IMAGE_HEIGHT - fm.getHeight()) / 2 + fm.getAscent();

        g2d.drawString(text, x, y);

        g2d.dispose();
        return image;
    }
    
    private void drawInterference(Graphics2D g) {
        g.setColor(new Color(200, 200, 200));
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            int x1 = random.nextInt(IMAGE_WIDTH);
            int y1 = random.nextInt(IMAGE_HEIGHT);
            int x2 = random.nextInt(IMAGE_WIDTH);
            int y2 = random.nextInt(IMAGE_HEIGHT);
            g.drawLine(x1, y1, x2, y2);
        }
    }
}
