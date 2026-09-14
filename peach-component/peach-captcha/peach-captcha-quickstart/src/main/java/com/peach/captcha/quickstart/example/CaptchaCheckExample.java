package com.peach.captcha.quickstart.example;

import com.peach.captcha.key.CaptchaRedisKey;
import com.peach.captcha.model.CaptchaVO;
import com.peach.captcha.service.CaptchaCacheService;
import com.peach.captcha.service.CaptchaService;
import com.peach.captcha.util.AesUtil;
import com.peach.common.key.KeyBuilder;
import com.peach.common.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;

/**
 * 注入 {@link CaptchaService#check(CaptchaVO)}，演示正确校验、错误答案与缓存失效。
 *
 * <p>正确校验需要前端提交加密答案。Quickstart 从 {@link CaptchaCacheService} 读取缓存以模拟该步骤，
 * 不把明文答案写入日志。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:10
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class CaptchaCheckExample {

    private static final String WRONG_ANSWER = "not-the-captcha";

    private final CaptchaService captchaService;
    private final CaptchaCacheService captchaCacheService;

    /**
     * 使用缓存中的正确答案完成一次校验，成功后返回二次校验令牌。
     *
     * @param generated {@link CaptchaService#get(CaptchaVO)} 的结果
     * @return 校验响应
     */
    public Response checkSuccess(CaptchaVO generated) {
        Response response = captchaService.check(newCheckRequest(generated, readCachedAnswer(generated.getToken())));
        log.info("captcha check success path, ok={}", response != null && response.isSuccess());
        return response;
    }

    /**
     * 提交错误答案，期望校验失败。
     *
     * @param generated 已生成的验证码
     * @return 校验响应
     */
    public Response checkWrongAnswer(CaptchaVO generated) {
        Response response = captchaService.check(newCheckRequest(generated, WRONG_ANSWER));
        log.info("captcha check wrong-answer path, failed={}", response != null && !response.isSuccess());
        return response;
    }

    /**
     * 删除缓存条目以模拟过期后校验，期望 {@code API_CAPTCHA_INVALID}。
     *
     * @param generated 已生成的验证码
     * @return 校验响应
     */
    public Response checkExpired(CaptchaVO generated) {
        captchaCacheService.delete(runningKey(generated.getToken()));
        Response response = captchaService.check(newCheckRequest(generated, WRONG_ANSWER));
        log.info("captcha check expired path, code={}", response == null ? null : response.getCode());
        return response;
    }

    /**
     * 使用从未生成过的 token 校验，期望与过期相同的失效语义。
     *
     * @return 校验响应
     */
    public Response checkMissingToken() {
        CaptchaVO request = new CaptchaVO();
        request.setClientUid("qs-missing");
        request.setCaptchaType(CaptchaGenerateExample.TEXT_TYPE);
        request.setToken("missing-token");
        request.setAnswer("not-a-valid-cipher");
        Response response = captchaService.check(request);
        log.info("captcha check missing-token path, code={}", response == null ? null : response.getCode());
        return response;
    }

    private CaptchaVO newCheckRequest(CaptchaVO generated, String plainAnswer) {
        CaptchaVO request = new CaptchaVO();
        request.setClientUid(generated.getClientUid());
        request.setCaptchaType(CaptchaGenerateExample.TEXT_TYPE);
        request.setToken(generated.getToken());
        request.setAnswer(encrypt(plainAnswer, generated.getSecretKey()));
        return request;
    }

    private String readCachedAnswer(String token) {
        String cached = captchaCacheService.get(runningKey(token));
        if (cached == null || !cached.contains("#")) {
            throw new IllegalStateException("captcha cache miss");
        }
        return cached.split("#", 2)[0];
    }

    private static String runningKey(String token) {
        return KeyBuilder.from(CaptchaRedisKey.RUNNING_CAPTCHA, token).getRealKey();
    }

    private static String encrypt(String plainAnswer, String secretKey) {
        try {
            return AesUtil.aesEncrypt(plainAnswer, secretKey);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("encrypt captcha answer failed", ex);
        }
    }
}
