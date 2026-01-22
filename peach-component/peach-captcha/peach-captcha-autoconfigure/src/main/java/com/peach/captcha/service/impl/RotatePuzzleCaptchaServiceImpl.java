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

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 旋转拼图验证码服务实现类
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/22 11:39
 */
@Slf4j
public class RotatePuzzleCaptchaServiceImpl extends AbstractCacheService {

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

        // 1. 获取旋转底图
        BufferedImage originalImage = CaptchaImageUtil.getRotateImage();
        if (originalImage == null) {
            log.error("init original rotate image error");
            return Response.fail("init original rotate image error");
        }

        // 2. 生成旋转图片
        // 随机旋转角度 0 - 360
        int randomAngle = RandomUtils.getRandomInt(0, 360);
        
        // 旋转图片
        BufferedImage rotatedImage = rotateImage(originalImage, randomAngle);
        
        // 3. 转换 Base64
        String rotatedImageBase64 = CaptchaImageUtil.getImageToBase64Str(rotatedImage);
        
        // 4. 生成 Token 和 缓存
        // 目标还原角度 = 360 - randomAngle
        int targetAngle = (360 - randomAngle) % 360;
        
        captchaVO.setRotateImageBase64(rotatedImageBase64);
        captchaVO.setToken(RandomUtils.getUuid());
        
        // 生成密钥
        String secretKey = AesUtil.getKey();
        if (CAPTCHA_AES_STATUS) {
            captchaVO.setSecretKey(secretKey);
        }
        
        String codeKey = RedisKeyBuild
                .createRedisKey(RedisKeyManage.RUNNING_CAPTCHA, captchaVO.getToken())
                .getRealKey();
        
        // 存储正确角度和密钥
        Map<String, Object> cacheMap = new HashMap<>();
        cacheMap.put("angle", targetAngle);
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
            return Response.fail(StatusEnum.API_CAPTCHA_INVALID);
        }

        String s = getCaptchaByKey(codeKey);
        // 取出后立即删除
        deleteCaptchKey(codeKey);

        if (StringUtil.isBlank(s)) {
             return Response.fail(StatusEnum.API_CAPTCHA_INVALID);
        }

        try {
            Map<String, Object> cacheMap = JSON.parseObject(s, Map.class);
            if (cacheMap == null) {
                return Response.fail("validate fail");
            }
            
            Integer targetAngle = (Integer) cacheMap.get("angle");
            String secretKey = (String) cacheMap.get("secretKey");
            
            // 解密用户提交的角度
            String pointJson = captchaVO.getAnswer();
            String decrypted = AesUtil.aesDecrypt(pointJson, secretKey);
            
            // 假设解密后是 JSON {"angle": 123} 或直接是角度字符串
            // 兼容直接传数字字符串的情况
            Double userAngleDouble;
            try {
                // 尝试解析JSON
                Map userMap = JSON.parseObject(decrypted, Map.class);
                if (userMap != null && userMap.containsKey("angle")) {
                    userAngleDouble = Double.parseDouble(userMap.get("angle").toString());
                } else {
                    userAngleDouble = Double.parseDouble(decrypted);
                }
            } catch (Exception e) {
                 userAngleDouble = Double.parseDouble(decrypted);
            }
            
            int userAngle = userAngleDouble.intValue();
            
            // 验证偏差 (允许 +- 5 度)
            int diff = Math.abs(userAngle - targetAngle);
            // 处理 0/360 边界问题
            if (diff > 180) {
                diff = 360 - diff;
            }
            
            if (diff > 5) {
                return Response.fail("validate fail");
            }
            
            // 生成二次校验
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
    
    /**
     * 图片旋转
     */
    private BufferedImage rotateImage(BufferedImage bufferedImage, int angle) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        
        // 创建新的图片（支持透明度）
        BufferedImage rotatedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = rotatedImage.createGraphics();
        
        // 设置抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        
        // 旋转中心
        int centerX = width / 2;
        int centerY = height / 2;
        
        AffineTransform transform = new AffineTransform();
        transform.rotate(Math.toRadians(angle), centerX, centerY);
        
        g2d.setTransform(transform);
        g2d.drawImage(bufferedImage, 0, 0, null);
        g2d.dispose();
        
        return rotatedImage;
    }
}
