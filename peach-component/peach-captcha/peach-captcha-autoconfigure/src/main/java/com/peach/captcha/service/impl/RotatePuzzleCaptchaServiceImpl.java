package com.peach.captcha.service.impl;

import com.alibaba.fastjson.JSON;
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

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * RotatePuzzle验证码服务实现类。
 * 旋转拼图验证码服务实现类
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/22 11:39
 */
@Slf4j
public class RotatePuzzleCaptchaServiceImpl extends AbstractCacheService {

    private static final String ANGLE_ATTRIBUTE = "angle";


    @Override
    public Response get(CaptchaVO captchaVO) {
        Response response = super.get(captchaVO);
        if (!validatedReq(response)) {
            return response;
        }

        // 1. Get original rotate image / 获取旋转底图
        BufferedImage originalImage = CaptchaImageUtil.getRotateImage();
        if (originalImage == null) {
            log.error("init original rotate image error");
            return Response.fail("init original rotate image error");
        }

        // 2. Generate rotated image / 生成旋转图片
        // Random rotation angle 0 - 360 / 随机旋转角度 0 - 360
        int randomAngle = RandomUtils.getRandomInt(0, 360);

        // Rotate image / 旋转图片
        BufferedImage rotatedImage = rotateImage(originalImage, randomAngle);

        // 3. Convert to Base64 / 转换 Base64
        String rotatedImageBase64 = CaptchaImageUtil.getImageToBase64Str(rotatedImage);
        // Also return the original image for verification/reference / 也返回原图用于验证/参考
        String originalImageBase64 = CaptchaImageUtil.getImageToBase64Str(originalImage);

        // 4. Generate Token and Cache / 生成 Token 和 缓存
        // Target restore angle = 360 - randomAngle / 目标还原角度 = 360 - randomAngle
        int targetAngle = (360 - randomAngle) % 360;

        captchaVO.setRotateImageBase64(rotatedImageBase64);
        // Use SlidingOriginalImageBase64 field to store the unrotated image / 使用 SlidingOriginalImageBase64 字段存储未旋转的原图
        captchaVO.setSlidingOriginalImageBase64(originalImageBase64);
        captchaVO.setToken(RandomUtils.getUuid());

        // Generate secret key / 生成密钥
        String secretKey = AesUtil.getKey();
        if (CAPTCHA_AES_STATUS) {
            captchaVO.setSecretKey(secretKey);
        }

        String codeKey = RedisKeyBuild
                .createRedisKey(RedisKeyManage.RUNNING_CAPTCHA, captchaVO.getToken())
                .getRealKey();

        // Store correct angle and secret key / 存储正确角度和密钥
        Map<String, Object> cacheMap = new HashMap<>();
        cacheMap.put(ANGLE_ATTRIBUTE, targetAngle);
        cacheMap.put("secretKey", secretKey);

        setCaptchaCahche(codeKey, JSON.toJSONString(cacheMap));

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

        String s = getCaptchaByKey(codeKey);
        // Delete immediately after retrieval / 取出后立即删除
        deleteCaptchKey(codeKey);

        if (StringUtil.isBlank(s)) {
            log.error("captcha check value empty, key: {}", codeKey);
             return Response.fail(StatusEnum.API_CAPTCHA_INVALID);
        }

        try {
            Map<String, Object> cacheMap = JSON.parseObject(s, Map.class);
            if (cacheMap == null) {
                log.error("captcha check cache map invalid");
                return Response.fail("validate fail");
            }

            Integer targetAngle = (Integer) cacheMap.get(ANGLE_ATTRIBUTE);
            String secretKey = (String) cacheMap.get("secretKey");

            // Decrypt user submitted angle / 解密用户提交的角度
            String pointJson = captchaVO.getAnswer();
            Double userAngleDouble = parseUserAngle(pointJson, secretKey);

            int userAngle = userAngleDouble.intValue();

            // Verify deviation (allow +- 5 degrees) / 验证偏差 (允许 +- 5 度)
            int diff = Math.abs(userAngle - targetAngle);
            // Handle 0/360 boundary / 处理 0/360 边界问题
            if (diff > 180) {
                diff = 360 - diff;
            }

            if (diff > 5) {
                return Response.fail("validate fail");
            }

            // Generate secondary verification / 生成二次校验
            String value = AesUtil.aesEncrypt(captchaVO.getToken().concat("@").concat(String.valueOf(userAngle)), secretKey);
            String secondKey = RedisKeyBuild
                .createRedisKey(RedisKeyManage.RUNNING_CAPTCHA_SECOND, value)
                .getRealKey();
            setCaptchaCahche(secondKey, captchaVO.getToken());

            captchaVO.setResult(true);
            captchaVO.setCaptchaVerification(value);
            return Response.success(captchaVO);

        } catch (Exception e) {
            log.error("Rotate check error", e);
            return Response.fail(e.getMessage());
        }
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
     * Rotate Image / 图片旋转
     */
    private BufferedImage rotateImage(BufferedImage bufferedImage, int angle) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        // Create new image (supports transparency) / 创建新的图片（支持透明度）
        BufferedImage rotatedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = rotatedImage.createGraphics();

        // Set antialiasing / 设置抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // Rotation center / 旋转中心
        int centerX = width / 2;
        int centerY = height / 2;

        AffineTransform transform = new AffineTransform();
        transform.rotate(Math.toRadians(angle), centerX, centerY);

        g2d.setTransform(transform);
        g2d.drawImage(bufferedImage, 0, 0, null);
        g2d.dispose();

        return rotatedImage;
    }

    private Double parseUserAngle(String pointJson, String secretKey) {
        try {
            String decrypted = AesUtil.aesDecrypt(pointJson, secretKey);
            Map<?, ?> userMap = JSON.parseObject(decrypted, Map.class);
            if (userMap != null && userMap.containsKey(ANGLE_ATTRIBUTE)) {
                return Double.parseDouble(userMap.get(ANGLE_ATTRIBUTE).toString());
            }
            return Double.parseDouble(decrypted);
        } catch (Exception e) {
            return null;
        }
    }
}
